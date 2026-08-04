// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        hot_article: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 500 },
                { duration: '1m', target: 2000 },
                { duration: '1m', target: 2000 },
                { duration: '30s', target: 0 },
            ],
        },
    },
};

const payload = JSON.stringify({
    lastCreatedAt: null,
    lastId: null,
    size: 12,
});

const params = {
    headers: { 'Content-Type': 'application/json' },
};

export default function () {
    const res = http.post(
        'http://localhost:8070/article/2076544116520132609/comment/getList',
        payload,
        params
    );
    check(res, { 'status 200': (r) => r.status === 200 });
    sleep(1);
}