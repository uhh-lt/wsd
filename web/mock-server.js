
var http = require("http");
http.createServer(function (request, response) {
      function sleep(milliseconds) {
        var start = new Date().getTime();
        for (var i = 0; i < 1e7; i++) {
          if ((new Date().getTime() - start) > milliseconds){
           break;
          }
        }
      }
      sleep(100000000);
      response.writeHead(200, {
         'Content-Type': 'text/plain'
      });
      response.write('Simple Simple Fun')
      response.end();
}).listen(5002);
