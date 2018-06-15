/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

const merge = require("webpack-merge");

const common = require("./webpack.common.js");

const API_HOST = process.env.API_HOST || "localhost";
const API_PORT = process.env.API_PORT || 8080;

module.exports = merge(common, {
    devtool: "inline-source-map",
    devServer: {
        hot: true,
        historyApiFallback: {
            index: "dev.html"
        },
        proxy: {
            "/api": {
                target: {
                    host: API_HOST,
                    protocol: "http:",
                    port: API_PORT
                }
            }
        }
    }
});
