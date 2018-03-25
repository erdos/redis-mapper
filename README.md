# erdos.redis-mapper

A data mapper library for Redis + Clojure.

## Usage

First, import the library.

```
(require '[erdos.redis-mapper :refer :all])
```

Second, define a schema and a validator function for your model. You can use *prismatic schema* for this purpose.

```
(def User
  {:name          String
   sc/Keyword     sc/Any})

(def user-validator (partial sc/validate User)
```

Next, define the model.

```
(db/defmodel user :validator (partial sc/validate User))
```

This generates the following functions:

- `->user` for creating new user instances and validating it.
- `->user!` for creating new user instances, validate then persist.
- `get-user` to query user instances by id.

### Creating instances

Create a new `user` instance and save it to the db instantly.

```
(def user-1 (->user! {:name "Mark"}))
```

Now `user-1` is just an immutable Clojure map. Try to modify it and persist immediately.

```
(def user-1-modified (persist! (assoc user-1 :name "Mark-1")))
```

This saves the user to the database overriding the previous snapshot.

## License

Copyright © 2018 Janos Erdos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
