Hey

You might be in here trying to figure out what MinecraftDataGenMixin is doing not being in the mixin file.

You might also be here wondering why `./gradlew runData` crashes.

Well, do I have a story for you!

Simple Voice Chat calls some stuff on initialization that you cannot call during client datagen.

I reported this on their Discord and hopefully they'll implement a fix. But we don't want to wait.

We depend on SVC due to the way we handle audio processing, PTT Mixin, etc.

We get broken datagen as a result. So! We have this great set of classes that Fixes It :tm:

So if you need to run datagen, please add `"MinecraftDataGenMixin"` to the `client: []` section.

Kinda like this:

```json
  "client": [
    "MinecraftDataGenMixin"
  ],
```

Technically, you can leave this mixin enabled and the code will work for `./gradlew runClient`.

But! It can cause performance regressions too! So we'd recommend you do NOT do that.