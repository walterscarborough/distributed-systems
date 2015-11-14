
'use strict';

// libs
var colors = require('colors/safe');
var net = require('net');
var util = require('util');
var uuid = require('uuid');

// app vars
var myIp = '';
var myPort = 0;
var myName = '';
var leftNeighborIp = '';
var leftNeighborPort = 0;
var sentMessages = new Set();

// command line arguments
/*
    0 - node
    1 - <filename>
    2 - <ip>
    3 - <port>
    4 - <name>
    5 - <leftNeighborIp>
    6 - <leftNeighborPort>
*/
if (process.argv.length < 7) {
    console.error('error - please supply all command line arguments.');
    return 1;
}
myIp = process.argv[2];
myPort = parseInt(process.argv[3]);
myName = process.argv[4];
leftNeighborIp = process.argv[5];
leftNeighborPort = parseInt(process.argv[6]);

// server
var server = net.createServer(function(socket) {
    //socket.write('Echo server\r\n');
    //socket.pipe(socket);

    socket.on('data', function(data) {
        data = data.toString().trim();

        if (sentMessages.has(data) === false) {

            var parsedData = JSON.parse(data);

            console.log(colors.green(parsedData.name + ' says: ' + parsedData.message));

            sendMessage(parsedData.message, parsedData.name, leftNeighborIp, leftNeighborPort);
        }
    });
});

server.listen(myPort, myIp);

// user input
process.stdin.resume();
process.stdin.setEncoding('utf8');

process.stdin.on('data', function (text) {

    if (text !== '\n') {

        text = text.replace('\n', '');

        var textSplit = text.split(" ");

        if (textSplit.length > 0) {

            /*
            if (textSplit[0] === 'addServer') {
                console.log("addServer hit");
            }
            else if (textSplit[0] === 'sendMessage') {

                textSplit.shift();

                var outText = textSplit.join(" ");

                console.log(colors.magenta('You say: ' + outText));

                sendMessage(outText, myName, leftNeighborIp, leftNeighborPort);
            }
            */

                var outText = textSplit.join(" ");

                console.log(colors.magenta('You say: ' + outText));

                sendMessage(outText, myName, leftNeighborIp, leftNeighborPort);
        }
    }
});

var sendMessage = function(text, name, ip, port) {

    // client
    var client = new net.Socket();

    client.connect(port, ip, function() {
        //console.log('client connected');

        var outgoingMessage = {
            'name': name,
            'message': text,
        };

        sentMessages.add(JSON.stringify(outgoingMessage));

        client.write(JSON.stringify(outgoingMessage));
    });

    client.on('data', function(data) {
        //console.log('client received: ' + data);
        client.destroy(); // kill client after server's response
    });

    client.on('close', function() {
        //console.log('Connection closed');
    });

    client.on('error', function(err){
        //console.log("Error: "+err.message);
    });
};
