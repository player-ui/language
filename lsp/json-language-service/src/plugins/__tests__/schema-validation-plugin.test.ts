import { test, expect, describe, beforeEach } from "vitest";
import { CommonTypes, Types } from "@player-lang/static-xlrs";
import { PlayerLanguageService } from "../..";
import { toTextDocument } from "../../utils";

describe("SchemaValidationPlugin", () => {
  let service: PlayerLanguageService;

  beforeEach(async () => {
    service = new PlayerLanguageService();
    await service.setAssetTypesFromModule([Types, CommonTypes]);
  });

  describe("schema structure validation", () => {
    test("reports error when schema is missing ROOT", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              SomeType: {
                foo: { type: "SomeType" },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - missing: Property "ROOT" missing from type "Schema"",
            "range": {
              "end": {
                "character": 3,
                "line": 12,
              },
              "start": {
                "character": 12,
                "line": 6,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema must have a "ROOT" key.",
            "range": {
              "end": {
                "character": 3,
                "line": 12,
              },
              "start": {
                "character": 12,
                "line": 6,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when DataType is missing type property", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                application: {
                  validation: [],
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - missing: Property "type" missing from type "DataType"",
            "range": {
              "end": {
                "character": 7,
                "line": 10,
              },
              "start": {
                "character": 21,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema.DataType must have a "type" property (reference to schema or XLR type).",
            "range": {
              "end": {
                "character": 7,
                "line": 10,
              },
              "start": {
                "character": 21,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when type is not a string", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                application: {
                  type: 123,
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected type "string" but got "number"",
            "range": {
              "end": {
                "character": 19,
                "line": 9,
              },
              "start": {
                "character": 8,
                "line": 9,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema "type" must be a string (schema type name or XLR type name).",
            "range": {
              "end": {
                "character": 19,
                "line": 9,
              },
              "start": {
                "character": 16,
                "line": 9,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when isArray is not a boolean", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                items: {
                  type: "StringType",
                  isArray: "yes",
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected type "boolean" but got "string"",
            "range": {
              "end": {
                "character": 24,
                "line": 10,
              },
              "start": {
                "character": 8,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema.DataType "isArray" must be a boolean.",
            "range": {
              "end": {
                "character": 24,
                "line": 10,
              },
              "start": {
                "character": 19,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - value: Unexpected properties on "StringType": isArray",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when validation is not an array", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                field: {
                  type: "TypeA",
                  validation: "not-an-array",
                },
              },
              TypeA: {
                nested: { type: "StringType" },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected an array but got an "string"",
            "range": {
              "end": {
                "character": 36,
                "line": 10,
              },
              "start": {
                "character": 22,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema.DataType "validation" must be an array.",
            "range": {
              "end": {
                "character": 36,
                "line": 10,
              },
              "start": {
                "character": 22,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when format is not an object", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                field: {
                  type: "TypeA",
                  format: "not-an-object",
                },
              },
              TypeA: {
                nested: { type: "StringType" },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected an object but got an "string"",
            "range": {
              "end": {
                "character": 33,
                "line": 10,
              },
              "start": {
                "character": 18,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema.DataType "format" must be an object.",
            "range": {
              "end": {
                "character": 33,
                "line": 10,
              },
              "start": {
                "character": 18,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 16,
              },
              "start": {
                "character": 16,
                "line": 14,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when isRecord is not a boolean", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                field: {
                  type: "StringType",
                  isRecord: "yes",
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Schema.DataType "isRecord" must be a boolean.",
            "range": {
              "end": {
                "character": 25,
                "line": 10,
              },
              "start": {
                "character": 20,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - value: Unexpected properties on "StringType": isRecord",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports error when both isArray and isRecord are true", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                field: {
                  type: "StringType",
                  isArray: true,
                  isRecord: true,
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Schema.DataType cannot have both "isArray" and "isRecord" true.",
            "range": {
              "end": {
                "character": 7,
                "line": 12,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 12,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 12,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 12,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - value: Unexpected properties on "StringType": isArray, isRecord",
            "range": {
              "end": {
                "character": 7,
                "line": 12,
              },
              "start": {
                "character": 15,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });
  });

  describe("schema type reference validation", () => {
    test("reports error for unknown type reference (not in schema, not in XLR)", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                foo: {
                  type: "NonExistentXLRType",
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Unknown schema type "NonExistentXLRType". Type must be a schema type (key in this schema) or an XLR type loaded in the SDK.",
            "range": {
              "end": {
                "character": 36,
                "line": 9,
              },
              "start": {
                "character": 16,
                "line": 9,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("accepts type reference to XLR-loaded type when that type is in the SDK", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                name: { type: "StringType" },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Schema DataType "StringType" - missing: Property "default" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 10,
              },
              "start": {
                "character": 14,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "validation" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 10,
              },
              "start": {
                "character": 14,
                "line": 8,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "StringType" - missing: Property "format" missing from type "StringType"",
            "range": {
              "end": {
                "character": 7,
                "line": 10,
              },
              "start": {
                "character": 14,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports errors for multiple unknown type references", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                a: { type: "UnknownA" },
                b: { type: "UnknownB" },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Unknown schema type "UnknownA". Type must be a schema type (key in this schema) or an XLR type loaded in the SDK.",
            "range": {
              "end": {
                "character": 26,
                "line": 9,
              },
              "start": {
                "character": 16,
                "line": 9,
              },
            },
            "severity": 1,
          },
          {
            "message": "Unknown schema type "UnknownB". Type must be a schema type (key in this schema) or an XLR type loaded in the SDK.",
            "range": {
              "end": {
                "character": 26,
                "line": 12,
              },
              "start": {
                "character": 16,
                "line": 12,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports no Schema DataType errors when DataType conforms to XLR (StringType)", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                name: {
                  type: "StringType",
                  default: "",
                  validation: [],
                  format: { type: "string" },
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected an object but got an "string"",
            "range": {
              "end": {
                "character": 21,
                "line": 10,
              },
              "start": {
                "character": 19,
                "line": 10,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });

    test("reports Schema DataType error when BooleanType payload has wrong type for property", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
            schema: {
              ROOT: {
                flag: {
                  type: "BooleanType",
                  default: "not-a-boolean",
                },
              },
            },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`
        [
          {
            "message": "Content Validation Error - type: Expected an object but got an "string"",
            "range": {
              "end": {
                "character": 34,
                "line": 10,
              },
              "start": {
                "character": 19,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "BooleanType" - type: Expected type "boolean" but got "string"",
            "range": {
              "end": {
                "character": 34,
                "line": 10,
              },
              "start": {
                "character": 8,
                "line": 10,
              },
            },
            "severity": 1,
          },
          {
            "message": "Schema DataType "BooleanType" - missing: Property "validation" missing from type "BooleanType"",
            "range": {
              "end": {
                "character": 7,
                "line": 11,
              },
              "start": {
                "character": 14,
                "line": 8,
              },
            },
            "severity": 1,
          },
        ]
      `);
    });
  });

  describe("flow without schema", () => {
    test("does not add schema errors when schema is absent", async () => {
      const document = toTextDocument(
        JSON.stringify(
          {
            id: "foo",
            views: [],
            navigation: { BEGIN: "FLOW1" },
          },
          null,
          2,
        ),
      );
      const diagnostics = await service.validateTextDocument(document);
      expect(diagnostics).toMatchInlineSnapshot(`[]`);
    });
  });
});
