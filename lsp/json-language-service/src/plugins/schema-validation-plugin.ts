import type { ValidationMessage } from "@xlr-lib/xlr-sdk";
import { DiagnosticSeverity } from "vscode-languageserver-types";
import type { PlayerLanguageService, PlayerLanguageServicePlugin } from "..";
import type { ValidationContext, ASTVisitor } from "../types";
import type { ContentASTNode, ObjectASTNode, StringASTNode } from "../parser";
import { findErrorNode, getProperty } from "../utils";
import type { XLRSDK } from "@xlr-lib/xlr-sdk";

/**
 * Collects all type names defined at the top level of the schema (ROOT and
 * any custom type names). Used to validate that "type" references either
 * point to a schema type or an XLR-loaded type.
 */
function getSchemaTypeNames(schemaObj: ObjectASTNode): Set<string> {
  const names = new Set<string>();
  for (const prop of schemaObj.properties) {
    const key = prop.keyNode?.value;
    if (typeof key === "string") {
      names.add(key);
    }
  }
  return names;
}

/**
 * Validates that a Schema.DataType object has the proper structure
 */
function validateDataTypeStructure(
  dataTypeNode: ObjectASTNode,
  validationContext: ValidationContext,
): void {
  const validationProp = getProperty(dataTypeNode, "validation");
  if (validationProp?.valueNode && validationProp.valueNode.type !== "array") {
    validationContext.addViolation({
      node: validationProp.valueNode,
      message: `Schema.DataType "validation" must be an array.`,
      severity: DiagnosticSeverity.Error,
    });
  }

  const formatProp = getProperty(dataTypeNode, "format");
  if (formatProp?.valueNode && formatProp.valueNode.type !== "object") {
    validationContext.addViolation({
      node: formatProp.valueNode,
      message: `Schema.DataType "format" must be an object.`,
      severity: DiagnosticSeverity.Error,
    });
  }

  const isArrayProp = getProperty(dataTypeNode, "isArray");
  const isRecordProp = getProperty(dataTypeNode, "isRecord");
  if (isArrayProp?.valueNode && isArrayProp.valueNode.type !== "boolean") {
    validationContext.addViolation({
      node: isArrayProp.valueNode,
      message: `Schema.DataType "isArray" must be a boolean.`,
      severity: DiagnosticSeverity.Error,
    });
  }
  if (isRecordProp?.valueNode && isRecordProp.valueNode.type !== "boolean") {
    validationContext.addViolation({
      node: isRecordProp.valueNode,
      message: `Schema.DataType "isRecord" must be a boolean.`,
      severity: DiagnosticSeverity.Error,
    });
  }
  if (
    isArrayProp?.valueNode &&
    isRecordProp?.valueNode &&
    (isArrayProp.valueNode as { value?: boolean }).value === true &&
    (isRecordProp.valueNode as { value?: boolean }).value === true
  ) {
    validationContext.addViolation({
      node: dataTypeNode,
      message: `Schema.DataType cannot have both "isArray" and "isRecord" true.`,
      severity: DiagnosticSeverity.Error,
    });
  }
}

/**
 * Validates a single schema node (e.g. ROOT or a custom type): each property
 * must be an object with a "type" field (Schema.DataType), full structure
 * validation, known type reference (schema or XLR), and when the type is an
 * XLR type, validates the DataType object against the XLR definition via the SDK.
 */
function validateSchemaNode(
  node: ObjectASTNode,
  schemaTypeNames: Set<string>,
  sdk: XLRSDK,
  validationContext: ValidationContext,
): void {
  for (const prop of node.properties) {
    const valueNode = prop.valueNode;
    if (!(valueNode && valueNode.type === "object")) {
      if (valueNode) {
        validationContext.addViolation({
          node: valueNode,
          message: `Schema property "${prop.keyNode.value}" must be an object (Schema.DataType) with a "type" field.`,
          severity: DiagnosticSeverity.Error,
        });
      }
      continue;
    }

    const dataTypeNode = valueNode as ObjectASTNode;
    const typeProp = getProperty(dataTypeNode, "type");
    if (!typeProp) {
      validationContext.addViolation({
        node: valueNode,
        message: `Schema.DataType must have a "type" property (reference to schema or XLR type).`,
        severity: DiagnosticSeverity.Error,
      });
      continue;
    }

    const typeValueNode = typeProp.valueNode;
    if (!typeValueNode || typeValueNode.type !== "string") {
      validationContext.addViolation({
        node: typeValueNode ?? typeProp,
        message: `Schema "type" must be a string (schema type name or XLR type name).`,
        severity: DiagnosticSeverity.Error,
      });
      continue;
    }

    const typeName = (typeValueNode as StringASTNode).value;
    const isSchemaType = schemaTypeNames.has(typeName);
    const isXLRType = sdk.hasType(typeName);

    if (!isSchemaType && !isXLRType) {
      validationContext.addViolation({
        node: typeValueNode,
        message: `Unknown schema type "${typeName}". Type must be a schema type (key in this schema) or an XLR type loaded in the SDK.`,
        severity: DiagnosticSeverity.Error,
      });
    }

    /** Full DataType structure per @player-ui/types */
    validateDataTypeStructure(dataTypeNode, validationContext);

    /** When type is an XLR type, validate the DataType object against the XLR definition */
    if (isXLRType) {
      const issues: ValidationMessage[] = sdk.validateByName(
        typeName,
        dataTypeNode.jsonNode,
      );
      issues.forEach((issue) => {
        validationContext.addViolation({
          node: findErrorNode(dataTypeNode, issue.node),
          message:
            issue.severity === DiagnosticSeverity.Error
              ? `Schema DataType "${typeName}" - ${issue.type}: ${issue.message}`
              : issue.message,
          severity: issue.severity as DiagnosticSeverity,
        });
      });
    }
  }
}

/**
 * Validates the Flow's schema property: structure per Schema.Schema,
 * type references, full DataType structure, and XLR shape when type is an XLR type.
 */
function validateFlowSchema(
  contentNode: ContentASTNode,
  sdk: XLRSDK,
  validationContext: ValidationContext,
): void {
  const schemaProp = getProperty(contentNode, "schema");
  if (!schemaProp?.valueNode) {
    return;
  }

  const schemaValue = schemaProp.valueNode;
  if (schemaValue.type !== "object") {
    validationContext.addViolation({
      node: schemaValue,
      message: `Flow "schema" must be an object with at least a "ROOT" key.`,
      severity: DiagnosticSeverity.Error,
    });
    return;
  }

  const schemaObj = schemaValue as ObjectASTNode;
  const hasRoot = schemaObj.properties.some((p) => p.keyNode.value === "ROOT");

  if (!hasRoot) {
    validationContext.addViolation({
      node: schemaValue,
      message: `Schema must have a "ROOT" key.`,
      severity: DiagnosticSeverity.Error,
    });
  }

  const schemaTypeNames = getSchemaTypeNames(schemaObj);

  for (const prop of schemaObj.properties) {
    const nodeValue = prop.valueNode;
    if (!nodeValue || nodeValue.type !== "object") {
      if (nodeValue) {
        validationContext.addViolation({
          node: nodeValue,
          message: `Schema node "${prop.keyNode.value}" must be an object.`,
          severity: DiagnosticSeverity.Error,
        });
      }
      continue;
    }

    validateSchemaNode(
      nodeValue as ObjectASTNode,
      schemaTypeNames,
      sdk,
      validationContext,
    );
  }
}

/**
 * Plugin that registers schema validation with the Player Language Service.
 */
export class SchemaValidationPlugin implements PlayerLanguageServicePlugin {
  name = "schema-validation";

  /** Resolved when CommonTypes have been loaded into the XLR SDK (once per plugin apply) */
  private commonTypesLoaded: Promise<void> | null = null;

  apply(service: PlayerLanguageService): void {
    service.hooks.validate.tap(this.name, async (_ctx, validationContext) => {
      await this.commonTypesLoaded;
      validationContext.useASTVisitor(
        this.createValidationVisitor(service, validationContext),
      );
    });
  }

  private createValidationVisitor(
    service: PlayerLanguageService,
    validationContext: ValidationContext,
  ): ASTVisitor {
    const sdk = service.XLRService.XLRSDK;
    return {
      ContentNode: (contentNode) => {
        validateFlowSchema(contentNode, sdk, validationContext);
      },
    };
  }
}
