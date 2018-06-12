/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

const path = require("path");

const BUILD_PATH = path.resolve(__dirname, "target", "classes", "META-INF",
    "resources", "webjars");

module.exports = {
    entry: path.resolve(__dirname, "app", "main.js"),
    output: {
        path: BUILD_PATH,
        filename: "bundle.js"
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                include: [
                    path.resolve(__dirname, "app")
                ],
                use: [
                    { loader: "babel-loader" }
                ]
            }
        ]
    }
};
