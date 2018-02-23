INSERT INTO person (id, name, uid, disabled, email) VALUES
  (1, 'test', '43,164,200,159', FALSE, 'test@example.com')
    ON CONFLICT DO NOTHING;

INSERT INTO checkin (id, auto, checked_in, time, person_id) VALUES
    (1, FALSE, TRUE, NOW() - INTERVAL '1 HOUR', 1),
    (2, FALSE, TRUE, NOW(), 1)
    ON CONFLICT DO NOTHING;