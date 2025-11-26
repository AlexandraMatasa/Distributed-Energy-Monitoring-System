import { HOST, endpoint } from '../../commons/api/host';
import RestApiClient from '../../commons/api/rest-client';

function getDailyConsumption(token, deviceId, date, callback) {
    let request = new Request(
        HOST.backend_api + endpoint.monitoring + '/device/' + deviceId + '/daily?date=' + date,
        {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        }
    );

    console.log('GET daily consumption request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

function getDeviceStats(token, deviceId, callback) {
    let request = new Request(
        HOST.backend_api + endpoint.monitoring + '/device/' + deviceId + '/stats',
        {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        }
    );

    console.log('GET device stats request to:', request.url);
    RestApiClient.performRequest(request, callback);
}

export default {
    getDailyConsumption,
    getDeviceStats
};