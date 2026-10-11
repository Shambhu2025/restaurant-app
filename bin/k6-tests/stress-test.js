import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 50 },   // warm up
        { duration: '20s', target: 100 },  // normal-ish load
        { duration: '20s', target: 200 },  // heavy load
        { duration: '20s', target: 300 },  // very heavy load
        { duration: '20s', target: 0 },    // ramp down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // more lenient than burst test - we WANT to see degradation
    http_req_failed: ['rate<0.20'],    // allow up to 20% failure before calling it a hard failure
  },
};

export function setup() {
  const res = http.get(`${BASE_URL}/restaurants`);
  const restaurants = JSON.parse(res.body);

  const restaurantsWithMenus = restaurants.map((r) => {
    const menuRes = http.get(`${BASE_URL}/menu?restaurantId=${r.id}`);
    const menuItems = JSON.parse(menuRes.body);
    return { restaurant: r, menuItems };
  }).filter((r) => r.menuItems.length > 0);

  return { restaurantsWithMenus };
}

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function (data) {
  const { restaurantsWithMenus } = data;
  const pick = randomFrom(restaurantsWithMenus);
  const restaurant = pick.restaurant;
  const menuItem = randomFrom(pick.menuItems);

  const action = Math.random();

  if (action < 0.5) {
    // 50% - browse (cheapest operation, most common in real usage)
    const res = http.get(`${BASE_URL}/restaurants`);
    check(res, { 'browse: status 200': (r) => r.status === 200 });

  } else if (action < 0.8) {
    // 30% - check a menu
    const res = http.get(`${BASE_URL}/menu?restaurantId=${restaurant.id}`);
    check(res, { 'menu: status 200': (r) => r.status === 200 });

  } else {
    // 20% - place an order (heaviest operation: writes to 2 tables, price lookups, transaction)
    const userPayload = JSON.stringify({
      email: `stress_${__VU}_${__ITER}_${Date.now()}@example.com`,
      password: 'password123',
    });
    const userRes = http.post(`${BASE_URL}/users`, userPayload, {
      headers: { 'Content-Type': 'application/json' },
    });

    if (userRes.status !== 201) {
      check(userRes, { 'order flow: user creation status 201': () => false });
      return;
    }

    const user = JSON.parse(userRes.body);
    const orderPayload = JSON.stringify({
      userId: user.id,
      restaurantId: restaurant.id,
      items: [{ menuItemId: menuItem.id, quantity: 1 }],
      deliveryAddress: '123 Stress St',
    });
    const orderRes = http.post(`${BASE_URL}/order`, orderPayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(orderRes, { 'place order: status 201': (r) => r.status === 201 });
  }

  sleep(0.1);
}