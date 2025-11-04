import { HOST, endpoint } from '../../commons/api/host';
import RestApiClient from '../../commons/api/rest-client';

function getDevices(token, callback) {
    let request = new Request(HOST.backend_api + endpoint.device, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('GET request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function getDeviceById(token, params, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/' + params.id, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('GET by ID request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function postDevice(token, device, callback) {
    let request = new Request(HOST.backend_api + endpoint.device, {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(device)
    });

    console.log('POST request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function updateDevice(token, device, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/' + device.id, {
        method: 'PUT',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(device)
    });

    console.log('PUT request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function deleteDevice(token, deviceId, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/' + deviceId, {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('DELETE request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function assignDeviceToUser(token, assignmentDTO, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/user', {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(assignmentDTO)
    });

    console.log('POST assignment request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function getDevicesByUserId(token, userId, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/user/' + userId, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('GET devices by user request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function unassignDevice(token, deviceId, callback) {
    let request = new Request(HOST.backend_api + endpoint.device + '/user/' + deviceId, {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    });

    console.log('DELETE assignment request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

export default {
    getDevices,
    getDeviceById,
    postDevice,
    updateDevice,
    deleteDevice,
    assignDeviceToUser,
    getDevicesByUserId,
    unassignDevice
};