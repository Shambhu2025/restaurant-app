import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const HEADERS = { headers: { 'Content-Type': 'application/json' } };

const orderAcceptMs = new Trend('order_accept_ms', true);
const orderConfirmMs = new Trend('order_confirm_ms', true);
const ordersAccepted = new Counter('orders_accepted');
const ordersNotSettled = new Counter('orders_not_settled_in_time');

export const options = {
  scenarios: {
    order_load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 300,
      stages: [
        { target: 50, duration: '20s' },   // steady load
        { target: 120, duration: '20s' },  // burst
      ],
    },
  },
  thresholds: {
    order_accept_ms: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export function setup() {
  const restaurants = JSON.parse(http.get(`${BASE_URL}/restaurants`).body).slice(0, 5);

  const targets = [];
  for (const r of restaurants) {
    const menu = JSON.parse(http.get(`${BASE_URL}/menu?restaurantId=${r.id}`).body);
    if (menu.length > 0) {
      targets.push({ restaurantId: r.id, menuItemIds: menu.map((m) => m.id) });
    }
  }

  const userRes = http.post(
    `${BASE_URL}/users`,
    JSON.stringify({
      email: `k6_orders_${Date.now()}@example.com`,
      password: 'password123',
      firstName: 'K6',
      lastName: 'Loader',
    }),
    HEADERS
  );

  return { targets, userId: JSON.parse(userRes.body).id };
}

export default function (data) {
  const target = pick(data.targets);
  const payload = JSON.stringify({
    userId: data.userId,
    restaurantId: target.restaurantId,
    items: [{ menuItemId: pick(target.menuItemIds), quantity: 1 + Math.floor(Math.random() * 3) }],
    deliveryAddress: 'K6 load test',
  });

  const res = http.post(`${BASE_URL}/order`, payload, HEADERS);
  orderAcceptMs.add(res.timings.duration);

  const accepted = check(res, { 'order accepted (202)': (r) => r.status === 202 });
  if (!accepted) return;
  ordersAccepted.add(1);

  // Sample 1 in 10 orders: poll until the consumer settles it, to measure end-to-end latency
  if (Math.random() < 0.1) {
    const orderId = JSON.parse(res.body).id;
    const started = Date.now();
    let settled = false;

    for (let i = 0; i < 120 && !settled; i++) {
      sleep(0.25);
      const poll = http.get(`${BASE_URL}/order/${orderId}`);
      if (poll.status === 200) {
        const status = JSON.parse(poll.body).status;
        if (status === 'CONFIRMED' || status === 'FAILED') {
          settled = true;
          orderConfirmMs.add(Date.now() - started);
          check(poll, { 'order confirmed': () => status === 'CONFIRMED' });
        }
      }
    }
    if (!settled) ordersNotSettled.add(1);
  }
}