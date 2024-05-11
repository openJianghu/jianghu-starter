package org.jianghu.app.common;

public class Constant {
    public static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String VALIDATE_SCHEMA_REQUEST_BODY = "{\n" +
            "                type: 'object',\n" +
            "                additionalProperties: true,\n" +
            "                required: [ 'packageId', 'packageType', 'appData' ],\n" +
            "                properties: {\n" +
            "                  packageId: { type: 'string' },\n" +
            "                  packageType: { type: 'string', enum: [ 'httpRequest' ] },\n" +
            "                  appData: {\n" +
            "                    type: 'object',\n" +
            "                    required: [ 'pageId', 'actionId' ],\n" +
            "                    properties: {\n" +
            "                      pageId: { type: 'string' },\n" +
            "                      actionId: { type: 'string' },\n" +
            "                      authToken: { anyOf: [{ type: 'string' }, { type: 'null' }] },\n" +
            "                      actionData: { type: 'object' },\n" +
            "                      where: { type: 'object' },\n" +
            "                    },\n" +
            "                  },\n" +
            "                },\n" +
            "              }";


}
