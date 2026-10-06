// The only file that talks to the Guess Market server (like ServerConnection + ServerApi in the JavaFX client).
// The screens call these functions the way they called the engine in exercise 2, and never deal with HTTP or JSON.
// Requests go to the address the page came from; the local server (server.js) forwards them to Tomcat.
// The browser keeps the server's session cookie (JSESSIONID) and sends it with every request by itself.
//
// Every function returns a Promise - a value that arrives later: `const me = await getMe();` waits for it
// without freezing the page. A failed request throws a ServerError instead.
// The addresses and parameter names are the server's (ApiPaths and ApiParams in the Java DTO module).

const BASE_URL = '/GuessMarket';
const TIMEOUT_MS = 10_000; // a server that does not answer within 10 seconds counts as unreachable

// A failed request: the HTTP status and a message that can be shown to the user as is.
export class ServerError extends Error {
    static NO_CONNECTION = 0;   // the page could not reach its local server (server.js), or no answer in time
    static CLIENT_FAILURE = -1; // a failure inside the web client itself (a bug)

    constructor(status, message) {
        super(message);
        this.status = status;
    }

    // Any error as a ServerError: a bug in the client (e.g. a TypeError) becomes CLIENT_FAILURE,
    // with the details in the browser's console (F12).
    static from(error) {
        if (error instanceof ServerError) {
            return error;
        }
        console.error(error);
        return new ServerError(ServerError.CLIENT_FAILURE, `Unexpected error: ${error.message}`);
    }

    // The session ended (logged out in another tab, or the session timeout passed): the user must log in again.
    isUnauthorized() {
        return this.status === 401;
    }

    // e.g. the name is used by a connected user, or the action does not fit the event's state
    isConflict() {
        return this.status === 409;
    }

    // 502: server.js is running, but it could not reach Tomcat
    isConnectionProblem() {
        return this.status === ServerError.NO_CONNECTION || this.status === 502;
    }
}

// --- Session ---

export function ping() {
    return get('/ping');
}

export function login(userName) {
    return post('/login', { username: userName });
}

export function logout() {
    return post('/logout');
}

// --- Users ---

export function getUsers() {
    return get('/users');
}

export function getMe() {
    return get('/me');
}

export function getMyEvent(eventName) {
    return get('/me/event', { event: eventName });
}

// The account's lines from index fromIndex on: only the new ones (delta fetching), like the JavaFX client.
export function getAccountEntries(fromIndex) {
    return get('/me/account', { from: fromIndex });
}

export function deposit(amount) {
    return post('/me/deposit', { amount });
}

// --- Events ---

export function getEvents() {
    return get('/events');
}

export function getParticipants(eventName) {
    return get('/event/participants', { event: eventName });
}

export function getOrderBook(eventName, outcome) {
    return get('/event/orderbook', { event: eventName, outcome });
}

export function openEvent(eventName) {
    return post('/event/open', { event: eventName });
}

export function closeEvent(eventName, winner) {
    return post('/event/close', { event: eventName, winner });
}

// --- Trading ---

export function buy(eventName, outcome, shares) {
    return post('/event/buy', { event: eventName, outcome, shares });
}

export function placeOrder(eventName, outcome, side, price, shares) {
    return post('/event/order', { event: eventName, outcome, side, price, shares });
}

// --- HTTP ---

// The parameters go in the address: /me/event?event=Rain%20in%20Eilat (URLSearchParams encodes spaces and symbols).
function get(path, params = {}) {
    const query = new URLSearchParams(params).toString();
    return send(query ? `${path}?${query}` : path, { method: 'GET' });
}

// The parameters go in the body, encoded like a browser form (the server reads them with getParameter).
function post(path, params = {}) {
    return send(path, { method: 'POST', body: new URLSearchParams(params) });
}

// Sends one request and returns the answer's JSON as a JavaScript object.
async function send(path, options) {
    let response;
    try {
        response = await fetch(BASE_URL + path, {
            ...options,
            headers: { Accept: 'application/json' },
            signal: AbortSignal.timeout(TIMEOUT_MS)
        });
    } catch (error) {
        throw new ServerError(ServerError.NO_CONNECTION, error.name === 'TimeoutError'
            ? 'The server did not answer in time. Please try again.'
            : 'The web client lost its connection. Make sure the run-web-client window is still open.');
    }

    const body = await readJson(response);
    if (!response.ok) {
        // Our server always answers errors as {"error": "..."}; anything else gets a general message.
        throw new ServerError(response.status,
            body?.error ?? `The server answered with an error (HTTP ${response.status}).`);
    }
    if (body === null) {
        throw new ServerError(ServerError.CLIENT_FAILURE, "The server's answer could not be read.");
    }
    return body;
}

async function readJson(response) {
    try {
        return await response.json();
    } catch {
        return null; // not JSON (for example Tomcat's own HTML error page)
    }
}
