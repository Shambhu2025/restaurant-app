import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  scenarios: {
    seed: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '5m',
    },
  },
};

const cuisines = ['Italian', 'American', 'Mexican', 'Indian', 'Chinese', 'Thai', 'Japanese'];
const restaurantNames = [
  'Pizza Palace', 'Burger Barn', 'Taco Town', 'Curry House', 'Wok Star',
  'Sushi Central', 'Noodle Bar', 'Grill Master', 'Spice Route', 'Pasta Point',
  'Fry Day', 'Bun Appetit', 'The Hungry Fork', 'Slice of Heaven', 'Bite Club',
  'Flame Kitchen', 'Urban Eats', 'Comfort Bites', 'The Daily Meal', 'Fork & Fire'
];
const menuItemNames = ['Classic Combo', 'House Special', 'Signature Dish', 'Chef\'s Choice', 'Daily Special'];

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomPrice(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2);
}

export function setup() {
  const restaurants = [];
  const menuItemsByRestaurant = {};

  // Create 20 restaurants
  for (let i = 0; i < restaurantNames.length; i++) {
    const payload = JSON.stringify({
      name: restaurantNames[i],
      description: `Great food at ${restaurantNames[i]}`,
      cuisine: randomFrom(cuisines),
      deliveryFee: randomPrice(1, 5),
      minOrderAmount: randomPrice(5, 15),
    });

    const res = http.post(`${BASE_URL}/restaurants`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });

    check(res, { 'restaurant created': (r) => r.status === 201 });

    const restaurant = JSON.parse(res.body);
    restaurants.push(restaurant);
    menuItemsByRestaurant[restaurant.id] = [];

    // Create 2-3 menu items per restaurant
    const itemCount = 2 + Math.floor(Math.random() * 2);
    for (let j = 0; j < itemCount; j++) {
      const itemPayload = JSON.stringify({
        name: `${randomFrom(menuItemNames)} ${j + 1}`,
        description: 'Delicious and freshly made',
        price: randomPrice(6, 20),
        calories: 300 + Math.floor(Math.random() * 700),
      });

      const itemRes = http.post(
        `${BASE_URL}/restaurants/${restaurant.id}/menu-items`,
        itemPayload,
        { headers: { 'Content-Type': 'application/json' } }
      );

      check(itemRes, { 'menu item created': (r) => r.status === 201 });
      menuItemsByRestaurant[restaurant.id].push(JSON.parse(itemRes.body));
    }
  }

  // Create 100 users
  const users = [];
  for (let i = 0; i < 100; i++) {
    const payload = JSON.stringify({
      email: `user${i}_${Date.now()}@example.com`,
      password: 'password123',
      firstName: `First${i}`,
      lastName: `Last${i}`,
      phone: `900000${String(i).padStart(4, '0')}`,
    });

    const res = http.post(`${BASE_URL}/users`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });

    check(res, { 'user created': (r) => r.status === 201 });
    users.push(JSON.parse(res.body));
  }

  console.log(`Seeded ${restaurants.length} restaurants and ${users.length} users`);

  return { restaurants, menuItemsByRestaurant, users };
}

export default function (data) {
  // Create 150 orders using the seeded data
  for (let i = 0; i < 150; i++) {
    const restaurant = randomFrom(data.restaurants);
    const menuItems = data.menuItemsByRestaurant[restaurant.id];
    const user = randomFrom(data.users);
    const menuItem = randomFrom(menuItems);

    const payload = JSON.stringify({
      userId: user.id,
      restaurantId: restaurant.id,
      items: [
        {
          menuItemId: menuItem.id,
          quantity: 1 + Math.floor(Math.random() * 3),
        },
      ],
      deliveryAddress: `${100 + i} Sample Street`,
      deliveryNotes: 'Leave at door',
    });

    const res = http.post(`${BASE_URL}/order`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });

    check(res, { 'order created': (r) => r.status === 201 });
    sleep(0.05); // small pause to avoid hammering the DB connection pool
  }

  console.log('Seeded 150 orders');
}