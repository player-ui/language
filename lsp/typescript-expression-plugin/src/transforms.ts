import type { FunctionType, FunctionTypeParameters } from "@xlr-lib/xlr";
import { simpleTransformGenerator } from "@xlr-lib/xlr-sdk";

export const toFunction = simpleTransformGenerator(
  "ref",
  "Expressions",
  (exp) => {
    if (!exp.genericArguments || exp.ref !== "ExpressionHandler") {
      return exp;
    }

    const [args, returnType] = exp.genericArguments;

    const parameters: Array<FunctionTypeParameters> = (
      args.type === "tuple" ? args.elementTypes : []
    ).map((elementType, index) => {
      return {
        name:
          elementType.name ??
          elementType.type.name ??
          elementType.type.title ??
          `arg_${index}`,

        type: {
          name:
            elementType.name ??
            elementType.type.name ??
            elementType.type.title ??
            `arg_${index}`,
          ...elementType.type,
        },
        optional:
          elementType.optional === true ? elementType.optional : undefined,
      };
    });

    return {
      ...exp,
      type: "function",
      parameters,
      returnType,
    } as FunctionType as any;
  },
);
