/**
 * Created by fide on 08.03.17.
 */

var path = require('path');
var express = require('express');
var morgan = require('morgan');

var url = require("url")

const homepage = process.env.PUBLIC_URL
    ? process.env.PUBLIC_URL
    : "";

base_url_path = homepage ? url.parse(homepage).pathname : "";

var app = express();
app.set('base', base_url_path);
var PORT = process.env.PORT || 8080;
app.use(morgan('combined'));

if(process.env.NODE_ENV !== 'production') {
    console.error("Only production mode supported")
} else {

    if(base_url_path) {
        app.use(base_url_path, express.static(path.join(__dirname, 'build')));
        app.get(base_url_path+'/*', function(request, response) {
            response.sendFile(__dirname + '/build/index.html')
        });
    } else {
        app.use(express.static(path.join(__dirname, 'build')));
        app.get('*', function(request, response) {
            response.sendFile(__dirname + '/build/index.html')
        });
    }


    app.listen(PORT, function(error) {
        if (error) {
            console.error(error);
        } else {
            console.info("==> Listening on port %s.", PORT);
        }
    });
}

