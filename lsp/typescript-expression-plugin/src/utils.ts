import ts from "typescript/lib/tsserverlibrary";
import type { TextSpan, SymbolDisplayPart } from "typescript";
import type { Position } from "vscode-languageserver-types";
import type { ExpressionNode, NodeLocation } from "@player-ui/player";
import type { NodeType } from "@xlr-lib/xlr";
import { isPrimitiveTypeNode } from "@xlr-lib/xlr-utils";
import { parseTree } from "jsonc-parser";
import type { Node } from "jsonc-parser";

const { SymbolDisplayPartKind } = ts;

/** Like `.join()` but for arrays */
function insertBetweenElements<T>(array: Array<T>, separator: T): T[] {
  return array.reduce((acc, item, index) => {
    if (index === 0) {
      return [item];
    }

    return [...acc, separator, item];
  }, [] as T[]);
}

/** Convert the TS SymbolDisplayParts into a single string */
export function symbolDisplayToString(
  displayParts: Array<SymbolDisplayPart>,
): string {
  return ts.displayPartsToString(displayParts);
}

/** Generate a documentation string for a given XLR node */
export function createTSDocString(node: NodeType): Array<SymbolDisplayPart> {
  if (node.type === "ref") {
    return [
      {
        text: node.ref,
        kind: SymbolDisplayPartKind.keyword as any,
      },
    ];
  }

  if (node.type === "or" || node.type === "and") {
    const items = node.type === "and" ? node.and : node.or;

    return insertBetweenElements(
      items.map((subnode) => createTSDocString(subnode)),
      [
        {
          kind: SymbolDisplayPartKind.punctuation as any,
          text: node.type === "and" ? " & " : " | ",
        },
      ],
    ).flat();
  }

  if (node.type === "function") {
    return [
      {
        kind: SymbolDisplayPartKind.keyword as any,
        text: "function",
      },
      {
        kind: SymbolDisplayPartKind.space as any,
        text: " ",
      },
      ...(node.name
        ? [{ text: node.name, kind: SymbolDisplayPartKind.methodName }]
        : []),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: "(",
      },
      ...insertBetweenElements(
        node.parameters.map((p) => {
          if (p.name) {
            return [
              {
                kind: SymbolDisplayPartKind.parameterName as any,
                text: p.name,
              },
              {
                kind: SymbolDisplayPartKind.punctuation as any,
                text: p.optional ? "?" : "",
              },
              {
                kind: SymbolDisplayPartKind.punctuation as any,
                text: ": ",
              },
              ...createTSDocString(p.type),
            ];
          }

          return createTSDocString(p.type);
        }),
        [
          {
            kind: SymbolDisplayPartKind.punctuation as any,
            text: ", ",
          },
        ],
      ).flat(),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: ")",
      },
      ...(node.returnType
        ? [
            {
              kind: SymbolDisplayPartKind.punctuation as any,
              text: ": ",
            },
            ...createTSDocString(node.returnType),
          ]
        : []),
    ];
  }

  if (node.type === "tuple") {
    return [
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: "[",
      },
      ...insertBetweenElements(
        node.elementTypes.map((t) => {
          if (t.name) {
            return [
              {
                kind: SymbolDisplayPartKind.propertyName as any,
                text: t.name,
              },
              {
                kind: SymbolDisplayPartKind.punctuation as any,
                text: ": ",
              },
              ...createTSDocString(t.type),
            ];
          }

          return createTSDocString(t.type);
        }),
        [
          {
            kind: SymbolDisplayPartKind.punctuation as any,
            text: ", ",
          },
        ],
      ).flat(),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: "]",
      },
    ];
  }

  if (node.type === "array") {
    return [
      {
        kind: SymbolDisplayPartKind.interfaceName as any,
        text: "Array",
      },
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: "<",
      },
      ...createTSDocString(node.elementType),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: ">",
      },
    ];
  }

  if (node.type === "record") {
    return [
      {
        kind: SymbolDisplayPartKind.interfaceName as any,
        text: "Record",
      },
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: "<",
      },
      ...createTSDocString(node.keyType),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: ", ",
      },
      ...createTSDocString(node.valueType),
      {
        kind: SymbolDisplayPartKind.punctuation as any,
        text: ">",
      },
    ];
  }

  if (
    (node.type === "string" ||
      node.type === "boolean" ||
      node.type === "number") &&
    node.const !== undefined
  ) {
    return [
      {
        kind: SymbolDisplayPartKind.keyword as any,
        text:
          typeof node.const === "string"
            ? `"${node.const}"`
            : String(node.const),
      },
    ];
  }

  if (isPrimitiveTypeNode(node) && node.type !== "null") {
    return [
      {
        kind: SymbolDisplayPartKind.keyword as any,
        text: node.type,
      },
    ];
  }

  if (node.type === "object" && node.name) {
    return [
      {
        kind: SymbolDisplayPartKind.interfaceName as any,
        text: node.name,
      },
    ];
  }

  return [
    {
      kind: SymbolDisplayPartKind.localName as any,
      text: node.type,
    },
  ];
}

/** Check if the vscode position overlaps with the expression location */
export function isInRange(position: Position, location: NodeLocation) {
  return (
    position.character >= location.start.character &&
    position.character <= location.end.character
  );
}

/** Find the closest marked token at the given position */
export function getTokenAtPosition(
  node: ExpressionNode,
  position: Position,
): ExpressionNode | undefined {
  if (node.type === "CallExpression") {
    const anyArgs = node.args.find((arg) => {
      return getTokenAtPosition(arg, position);
    });

    if (anyArgs) {
      return anyArgs;
    }

    const asTarget = getTokenAtPosition(node.callTarget, position);
    if (asTarget) {
      return asTarget;
    }
  }

  if (node.type === "Assignment") {
    const asTarget =
      getTokenAtPosition(node.left, position) ??
      getTokenAtPosition(node.right, position);
    if (asTarget) {
      return asTarget;
    }
  }

  // Lastly check for yourself
  if (node.location && isInRange(position, node.location)) {
    return node;
  }
}

/** Get the location info that TS expects for it's diags */
export function toTSLocation(node: ExpressionNode): TextSpan {
  const start = node.location?.start.character;
  const end = node.location?.end.character;
  if (start === undefined || end === undefined) {
    return {
      start: 0,
      length: 0,
    };
  }

  return {
    start,
    length: end - start,
  };
}

/** ExpressionNode -> raw value */
export function convertExprToValue(exprNode: ExpressionNode): any {
  let val;

  if (exprNode.type === "Literal") {
    val = exprNode.value;
  } else if (exprNode.type === "Object") {
    val = {};
    exprNode.attributes.forEach((prop) => {
      val[convertExprToValue(prop.key)] = convertExprToValue(prop.value);
    });
  } else if (exprNode.type === "ArrayExpression") {
    val = exprNode.elements.map(convertExprToValue);
  }

  return val;
}

/** ExpressionNode -> JSONC Node */
export function convertExprToJSONNode(
  exprNode: ExpressionNode,
): Node | undefined {
  const val = convertExprToValue(exprNode);
  if (val === undefined) {
    return undefined;
  }

  return parseTree(JSON.stringify(val));
}
