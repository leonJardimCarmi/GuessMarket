// The web client's local server (Node.js, built-in modules only - nothing to install). It has two jobs:
//  1. Serve the web client's files (the "public" folder) to the browser.
//  2. Forward every request for /GuessMarket/... to the Guess Market server (Tomcat) and pass its answer back as is.
// The browser gets both from the same address, so the same-origin policy lets the page read the server's answers
// and keep the server's session cookie - and the Tomcat server needs no change at all.

import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { exec } from 'node:child_process';

const PORT = 3000;
const ADDRESS = `http://localhost:${PORT}`;

// The Guess Market server, at the same address the JavaFX client uses.
// 127.0.0.1 is "localhost" written as a number, so Node never tries the IPv6 form first.
const SERVER_HOST = '127.0.0.1';
const SERVER_PORT = 8080;
const CONTEXT_PATH = '/GuessMarket';
const SERVER_ADDRESS = `http://localhost:${SERVER_PORT}${CONTEXT_PATH}`;

const PUBLIC_FOLDER = path.join(path.dirname(fileURLToPath(import.meta.url)), 'public');
const CONTENT_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.ico': 'image/x-icon'
};

// "localhost" means 127.0.0.1 (IPv4) or ::1 (IPv6), and a browser may try either one, so we listen on both.
// Only these loopback addresses: other computers cannot connect, and Windows Firewall has nothing to ask about.
try {
    await listen('127.0.0.1');
} catch (error) {
    exitWithError(error);
}
try {
    await listen('::1');
} catch (error) {
    if (error.code === 'EADDRINUSE') {
        exitWithError(error);
    }
    // any other error: this computer has no IPv6 - IPv4 is enough
}

console.log('Guess Market web client is running.');
console.log(`Open ${ADDRESS} in your browser.`);
console.log(`Requests to ${CONTEXT_PATH} are forwarded to the Guess Market server at ${SERVER_ADDRESS}`);
console.log('Keep this window open while you use the web client. Press Ctrl+C to stop it.');

// run-web-client.bat starts the server with --open: then we also open the default browser at our address.
if (process.argv.includes('--open') && process.platform === 'win32') {
    exec(`start "" "${ADDRESS}"`);
}

function listen(address) {
    return new Promise((resolve, reject) => {
        const server = http.createServer(handleRequest);
        server.once('error', reject);
        server.listen(PORT, address, resolve);
    });
}

function exitWithError(error) {
    if (error.code === 'EADDRINUSE') {
        console.error(`Port ${PORT} is already in use. Is the web client already running in another window?`);
        console.error('Close that window (or the other program that uses the port) and try again.');
    } else {
        console.error(`The web client could not start: ${error.message}`);
    }
    process.exit(1);
}

function handleRequest(request, response) {
    if (request.url === CONTEXT_PATH || request.url.startsWith(CONTEXT_PATH + '/')) {
        forwardToServer(request, response);
    } else {
        sendFile(request, response);
    }
}

// Sends the request to Tomcat exactly as it came (method, address, headers incl. the session cookie, body)
// and streams Tomcat's answer back exactly as it came (status, headers incl. Set-Cookie, body).
function forwardToServer(request, response) {
    const options = {
        host: SERVER_HOST,
        port: SERVER_PORT,
        method: request.method,
        path: request.url,
        headers: { ...request.headers, host: `localhost:${SERVER_PORT}` }
    };
    const serverRequest = http.request(options, serverResponse => {
        response.writeHead(serverResponse.statusCode, serverResponse.headers);
        serverResponse.pipe(response);
    });
    serverRequest.on('error', () => {
        if (response.headersSent || response.destroyed) {
            response.destroy();
            return;
        }
        // The same shape as the server's own errors ({"error": "..."}), so the page shows it like any other error.
        sendJson(response, 502, {
            error: `Cannot reach the Guess Market server at ${SERVER_ADDRESS}. Make sure Tomcat is running.`
        });
    });
    request.pipe(serverRequest);
}

// Answers with a file from the public folder; "/" means index.html.
function sendFile(request, response) {
    if (request.method !== 'GET' && request.method !== 'HEAD') {
        sendText(response, 405, 'Method not allowed.');
        return;
    }
    let urlPath;
    try {
        urlPath = decodeURIComponent(new URL(request.url, ADDRESS).pathname);
    } catch {
        sendText(response, 400, 'Bad address.');
        return;
    }
    const filePath = path.join(PUBLIC_FOLDER, urlPath === '/' ? 'index.html' : urlPath);
    // Never serve anything outside the public folder (for example a request for /../server.js).
    if (!filePath.startsWith(PUBLIC_FOLDER + path.sep)) {
        sendText(response, 404, 'Not found.');
        return;
    }
    fs.readFile(filePath, (error, content) => {
        if (error) {
            sendText(response, 404, 'Not found.');
            return;
        }
        response.writeHead(200, {
            'Content-Type': CONTENT_TYPES[path.extname(filePath).toLowerCase()] ?? 'application/octet-stream',
            'Cache-Control': 'no-cache' // the browser checks for a newer version every time: an update is never missed
        });
        response.end(request.method === 'HEAD' ? undefined : content);
    });
}

function sendJson(response, status, body) {
    response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
    response.end(JSON.stringify(body));
}

function sendText(response, status, text) {
    response.writeHead(status, { 'Content-Type': 'text/plain; charset=utf-8' });
    response.end(text);
}
