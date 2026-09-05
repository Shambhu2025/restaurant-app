import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  scenarios: {
    burst: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 100 },  // sudden spike to 100 concurrent users
        { duration: '20s', target: 100 }, // hold at 100 for 20 seconds
        { duration: '5s', target: 0 },    // ramp back down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% of requests should complete under 1s
    http_req_failed: ['rate<0.05'],    // less than 5% of requests should fail
  },
};

// Restaurant/menu item IDs from your seeded data - we'll fetch them dynamically in setup()
export function setup() {
  const res = http.get(`${BASE_URL}/restaurants`);
  const restaurants = JSON.parse(res.body);

  const restaurantsWithMenus = restaurants.map((r) => {
    const menuRes = http.get(`${BASE_URL}/menu?restaurantId=${r.id}`);
    const menuItems = JSON.parse(menuRes.body);
    return { restaurant: r, menuItems };
  }).filter((r) => r.menuItems.length > 0);

  const usersRes = http.get(`${BASE_URL}/restaurants`); // reuse - just need any valid data
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

  // Simulate a realistic mix of traffic
  const action = Math.random();

  if (action < 0.4) {
    // 40% - browse all restaurants
    const res = http.get(`${BASE_URL}/restaurants`);
    check(res, { 'browse restaurants: status 200': (r) => r.status === 200 });

  } else if (action < 0.7) {
    // 30% - view one restaurant
    const res = http.get(`${BASE_URL}/restaurants/${restaurant.id}`);
    check(res, { 'view restaurant: status 200': (r) => r.status === 200 });

  } else if (action < 0.9) {
    // 20% - check a menu
    const res = http.get(`${BASE_URL}/menu?restaurantId=${restaurant.id}`);
    check(res, { 'view menu: status 200': (r) => r.status === 200 });

  } else {
    // 10% - place an order (using a random seeded user id would need a fetch;
    // for simplicity we reuse a fixed test user pattern by creating one inline)
    const userPayload = JSON.stringify({
      email: `burst_${__VU}_${__ITER}_${Date.now()}@example.com`,
      password: 'password123',
    });
    const userRes = http.post(`${BASE_URL}/users`, userPayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    const user = JSON.parse(userRes.body);

    const orderPayload = JSON.stringify({
      userId: user.id,
      restaurantId: restaurant.id,
      items: [{ menuItemId: menuItem.id, quantity: 1 }],
      deliveryAddress: '123 Burst St',
    });
    const orderRes = http.post(`${BASE_URL}/order`, orderPayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(orderRes, { 'place order: status 201': (r) => r.status === 201 });
  }

  sleep(0.1);
}