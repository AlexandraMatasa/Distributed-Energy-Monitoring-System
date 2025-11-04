import { HOST, endpoint } from './host';
import RestApiClient from './rest-client';

function login(credentials, callback) {
    let request = new Request(HOST.backend_api + endpoint.auth + '/login', {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(credentials)
    });

    console.log('Login request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

export default {
    login
};