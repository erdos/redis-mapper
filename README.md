# erdos.redis-mapper

A data mapper library for Redis + Clojure.

## Usage

Add the dependency to your `project.clj` file: `[io.github.erdos/redis-mapper "0.1.0"]`

To use the library from a Clojure namespace, first, require the library.

```
(require '[erdos.redis-mapper :refer :all])
```

## Overview

- `(defmodel car :indices [:color :brand] :validator val-fn)` - creates a new model called *car* with indices on keys `:color` and `:brand` and validator function `val-fn`.
- `(persist! x)` - persists an entity to db. Returns updated entity.
- `(->car c)` - given a model called *car* and the data in *c* created a new *car* instance. Calls validator function first.
- `(->car! c)` - given a model called *car* and the data in *c* creates a new *car* instance and persistst, too.
- `(get-car ID)` - returns a *car* instance with the given identifier.
- `(get-car :color "red")` - returns a seq of *red* cars.
- `(get-first-car :color "red")` - returns one car instance with *red* color property.
- `(->id car-instance)` - returns the identifier of a *car* instance.

The entities are simple persistent maps so you can call the usual functions on them: `assoc`, `assoc-in`, `get`, `get-in`, `update`, `update-in`, `merge`, `into`, etc. Call `persist!` to save the changes to the database. Call `revert` to revert modifications and return the original entity from the db.

## Examples

You can define a schema and a validator function for your models. For example, you can use **prismatic schema** for this purpose.

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
- `get-user` to query user instances by id or indices.
- `get-first-user` to query user instances by some index.

### Creating and persisting

Create a new `user` instance and save it to the db instantly:

```
(def user-1 (->user! {:name "Mark"}))
```

Now `user-1` is just an immutable Clojure map. Try to modify it and persist immediately.

```
(def user-1-modified (persist! (assoc user-1 :name "Mark-1")))
```

This saves the user to the database overriding the previous snapshot.

### Reverting

Call the `(revert x)` function on a changed map to revert it to the original version found in the db.

```
(-> user-1-modified
    (assoc :x 2314)
    (assoc :name "Change")
    ;; ... ok, here i change my mind, i want the original back
    (revert)
    (= user-1-modified)) ;; => true
```

You can always revert to the latest version persisted to the db.

### Identifier and lookup

You can get the identifier of an already persisted entity using the `->id` function.

```
(println (->id user-1-modified)) ;;  this will print an integer
```

Use the identifier value to retrieve an entity later:

```
(get-user (->id user-1-modified)) ;; return the user object from the db
```

### Indices

To use indexed values, specify the list of indices during the definition of the model.

```
(db/defmodel car :indices [:color :brand])
```

To return a sequence of all the red cars just type:

```
(get-car :color "red")
```

If the indexed key is missing from the object it will not be inserted into the index structure.
If the key is present but has a `nil` value then the `nil` value will be indexed.

```
(->car! {:color nil})
(->car! {:name "beetle"})
(count (get-car :color nil)) ;; == 1 because only the first one is indexed
```

Use `(get-first-car :color "red")` to find only one *car* entity with the *color* value *red*.

# Development

- Use Leiningen (>2.0.0) for development.
- Call `$ lein test` to run all unit tests.
- Call `$ lein quickie` for continuous live testing.

## License

Copyright © 2018 Janos Erdos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
