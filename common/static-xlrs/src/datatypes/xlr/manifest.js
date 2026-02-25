const BooleanType = require("./BooleanType.json")
const IntegerType = require("./IntegerType.json")
const IntegerPosType = require("./IntegerPosType.json")
const IntegerNNType = require("./IntegerNNType.json")
const StringType = require("./StringType.json")
const CollectionType = require("./CollectionType.json")
const DateType = require("./DateType.json")
const PhoneType = require("./PhoneType.json")

    module.exports = {
      "pluginName": "CommonTypes",
      "capabilities": {
        "Assets":[],
		"Views":[],
		"Expressions":[],
		"DataTypes":[BooleanType,IntegerType,IntegerPosType,IntegerNNType,StringType,CollectionType,DateType,PhoneType],
      },
      "customPrimitives": [
        "Expression","Asset","Binding","AssetWrapper","Schema.DataType","ExpressionHandler"
      ]
    }
