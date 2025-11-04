import { HOST, endpoint } from '../../commons/api/host';
import RestApiClient from '../../commons/api/rest-client';

function getUsers(token, callback) {
    let request = new Request(HOST.backend_api + endpoint.user, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('GET request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function getUserById(token, params, callback) {
    let request = new Request(HOST.backend_api + endpoint.user + '/' + params.id, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('GET by ID request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function postUser(token, user, callback) {
    let request = new Request(HOST.backend_api + endpoint.auth + '/register', {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(user)
    });

    console.log('POST request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function updateUser(token, user, callback) {
    let request = new Request(HOST.backend_api + endpoint.user + '/' + user.id, {
        method: 'PUT',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(user)
    });

    console.log('PUT request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function deleteUser(token, userId, callback) {
    let request = new Request(HOST.backend_api + endpoint.user + '/' + userId, {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('DELETE request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

export default {
    getUsers,
    getUserById,
    postUser,
    updateUser,
    deleteUser
};