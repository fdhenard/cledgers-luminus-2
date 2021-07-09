-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id



-- :name create-xaction! :! :n
-- :doc creates a new xaction
INSERT INTO xaction
  (description, amount, date, created_by_id, uuid, payee_id, ledger_id)
VALUES
  (:description, :amount, :date, :created-by-id, :uuid, :payee-id, :ledger-id)

-- :name create-payee! :returning-execute :1
-- :doc creates a new payee
INSERT INTO payee
  (name, created_by_id)
VALUES
  (:name, :created-by-id)
RETURNING id;

-- :name create-ledger! :returning-execute :1
INSERT INTO ledger
  (name, created_by_id)
VALUES
  (:name, :created-by-id)
RETURNING id;

-- :name get-frank-id :? :1
SELECT id FROM cledgers_user
 WHERE username = 'frank'
