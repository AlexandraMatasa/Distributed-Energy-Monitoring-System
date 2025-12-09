import React, { useState, useEffect, useRef, useCallback} from 'react';
import { Card, CardHeader, CardBody, Form, FormGroup, Label, Input, Alert } from 'reactstrap';
import { Line } from 'react-chartjs-2';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
} from 'chart.js';
import API_MONITORING from '../api/monitoring-api';
import WebSocketService from '../services/WebSocketService';

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
);

function EnergyChart({ device, token }) {
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [chartData, setChartData] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    const currentDeviceId = useRef(null);
    const selectedDateRef = useRef(selectedDate);
    const isFetchingRef = useRef(false);
    const chartRef = useRef(null);
    const updateTimeoutRef = useRef(null);

    useEffect(() => {
        selectedDateRef.current = selectedDate;
    }, [selectedDate]);

    const fetchData = useCallback(() => {
        if (!device || !device.id) return;

        if (isFetchingRef.current) {
            console.log('Fetch already in progress, skipping...');
            return;
        }

        isFetchingRef.current = true;
        setIsLoading(true);
        setError(null);

        console.log('Fetching data for device:', device.id, 'date:', selectedDate);

        API_MONITORING.getDailyConsumption(token, device.id, selectedDate, (result, status, err) => {
            isFetchingRef.current = false;
            setIsLoading(false);

            if (result !== null && status === 200) {
                console.log('Data fetched successfully. Records:', result.length);
                processChartData(result);
            } else {
                console.error('Failed to fetch data:', err);
                setError(err?.message || 'Failed to load consumption data');
                setChartData(null);
            }
        });
    }, [device?.id, selectedDate, token]);

    const debouncedFetch = useCallback(() => {
        if (updateTimeoutRef.current) {
            clearTimeout(updateTimeoutRef.current);
        }

        updateTimeoutRef.current = setTimeout(() => {
            fetchData();
        }, 500);
    }, [fetchData]);

    useEffect(() => {
        if (!device || !device.id) return;

        console.log('Device or date changed, fetching initial data...');
        fetchData();

        return () => {
            if (updateTimeoutRef.current) {
                clearTimeout(updateTimeoutRef.current);
            }
        };
    }, [device?.id, selectedDate]);

    useEffect(() => {
        if (!device || !device.id) return;

        if (currentDeviceId.current && currentDeviceId.current !== device.id) {
            console.log('Device changed, disconnecting WebSocket...');
            WebSocketService.disconnect();
        }

        currentDeviceId.current = device.id;
        const listenerId = 'energy-chart-' + device.id;

        const handleWSMessage = (type, data) => {
            console.log('WebSocket event:', type, data);

            if (type === 'newMeasurement') {
                try {
                    console.log('New measurement received:', data);

                    const incomingHour = data.data?.hour;

                    if (!incomingHour) {
                        console.warn('Received update without required hour field.');
                        return;
                    }

                    const incomingDate = incomingHour.split('T')[0];
                    const currentSelectedDate = selectedDateRef.current;

                    console.log('Date comparison:', {
                        incoming: incomingDate,
                        selected: currentSelectedDate,
                        match: incomingDate === currentSelectedDate
                    });

                    if (incomingDate === currentSelectedDate) {
                        console.log('Date matches! Scheduling debounced refresh...');
                        debouncedFetch();
                    } else {
                        console.log('Date does not match, skipping refresh');
                    }
                } catch (e) {
                    console.error('Error parsing WebSocket data:', e, data);
                }
            }
        };

        WebSocketService.addListener(listenerId, handleWSMessage);
        WebSocketService.connect(device.id);

        return () => {
            console.log('Cleaning up WebSocket listener for device:', device.id);
            WebSocketService.removeListener(listenerId);
        };
    }, [device?.id, debouncedFetch]);

    const processChartData = (hourlyData) => {
        const hours = Array.from({ length: 24 }, (_, i) => i);
        const consumptionMap = new Map();

        hourlyData.forEach(record => {
            const hour = new Date(record.hour).getHours();
            consumptionMap.set(hour, record.totalConsumption);
        });

        const data = hours.map(hour => consumptionMap.get(hour) || 0);

        setChartData({
            labels: hours.map(h => `${h}:00`),
            datasets: [
                {
                    label: 'Energy Consumption (kWh)',
                    data: data,
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.5)',
                    tension: 0.1
                }
            ]
        });
    };

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { position: 'top' },
            title: {
                display: true,
                text: `Energy Consumption for ${device?.name} on ${selectedDate}`
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return `${context.dataset.label}: ${context.parsed.y.toFixed(3)} kWh`;
                    }
                }
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                title: { display: true, text: 'Energy (kWh)' }
            },
            x: {
                title: { display: true, text: 'Hour of Day' }
            }
        },
        animation: {
            duration: 400,
            easing: 'easeInOutQuart'
        },
        transitions: {
            active: {
                animation: {
                    duration: 200
                }
            }
        }
    };

    if (!device) {
        return <div>No device selected</div>;
    }

    return (
        <Card className="mb-4">
            <CardHeader>
                <strong>Energy Consumption Chart - {device.name}</strong>
            </CardHeader>
            <CardBody>
                <FormGroup className="mb-3">
                    <Label for="dateSelect">Select Date</Label>
                    <Input
                        id="dateSelect"
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                    />
                </FormGroup>

                {error && (
                    <Alert color="danger">
                        {error}
                    </Alert>
                )}

                {isLoading && !chartData && (
                    <div className="text-center py-5">
                        <div className="spinner-border text-primary" role="status">
                            <span className="visually-hidden">Loading...</span>
                        </div>
                    </div>
                )}

                {chartData && (
                    <div style={{ height: '400px', position: 'relative' }}>
                        <Line
                            ref={chartRef}
                            data={chartData}
                            options={chartOptions}
                        />
                    </div>
                )}

                {!chartData && !isLoading && !error && (
                    <div className="text-center py-5">
                        <p className="text-muted">No data available for the selected date</p>
                    </div>
                )}
            </CardBody>
        </Card>
    );
}

export default EnergyChart;