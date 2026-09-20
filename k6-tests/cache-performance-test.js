import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  scenarios: {
    cache_perf: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
    },
  },
};

export function setup() {
  const res = http.get(`${BASE_URL}/restaurants`);
  const restaurants = JSON.parse(res.body);
  // Use only a handful of restaurants repeatedly, to maximize realistic cache reuse
  const sample = restaurants.slice(0, 5);
  return { restaurantIds: sample.map((r) => r.id) };
}

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function (data) {
  const restaurantId = randomFrom(data.restaurantIds);

  const listRes = http.get(`${BASE_URL}/restaurants`);
  check(listRes, { 'restaurants: status 200': (r) => r.status === 200 });

  const menuRes = http.get(`${BASE_URL}/menu?restaurantId=${restaurantId}`);
  check(menuRes, { 'menu: status 200': (r) => r.status === 200 });

  sleep(0.1);
}