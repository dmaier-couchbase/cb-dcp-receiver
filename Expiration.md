* Creating somde documents with an expiration time

```
       List<JsonDocument> docs = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            
              docs.add(JsonDocument.create("key::" + i, 60, JsonObject.fromJson("{ \"content\" : \"This is the document #" + i  +"\"}")));    
        }
        
        Observable.from(docs).flatMap(d -> bucket.upsert(d)).subscribe(
        
                res -> LOG.log(Level.INFO, "Created document with id {0}", res.id())
        );
```

