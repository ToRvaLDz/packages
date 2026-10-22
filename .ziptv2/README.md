# Branch `ziptv2`

Fork di [flutter/packages](https://github.com/flutter/packages) usato da ZipTv2
(`ToRvaLDz/ziptv2`) per `packages/video_player/video_player_android`: API Media3
per tracce, cue testuali e transfer colore (ziptv2#551, ziptv2#563). Il branch è
**un tag upstream `video_player_android-vX.Y.Z` + i nostri commit**; `ziptv2` è
l'ultima versione **approvata**, `ziptv2-next` il candidato in prova. ZipTv2 lo usa
come `dependency_overrides` git con `ref:` fissato a uno SHA in `app/pubspec.yaml`.

## Aggiornamento: candidato testato + promozione via PR (ziptv2#554)

Il workflow `ziptv2 plugin` gira il lunedì alle 02:00 UTC e a mano
(`workflow_dispatch`; `force` ripropone un candidato già rifiutato).

1. **Pre-check senza clone**: `git ls-remote` dei tag upstream contro la versione
   del pubspec del plugin su `ziptv2`. Nessun tag nuovo = run finito in pochi
   secondi; il resto parte solo quando esce un tag (circa una volta al mese).
2. **Promozione**: se `ref:` in ziptv2 punta a un candidato costruito dal
   `ziptv2` attuale (tag `ziptv2/<versione>-<nostro>`, messaggio `base=<sha>`),
   `ziptv2` avanza a quel candidato.
3. **Candidato**: `git rebase --onto <nuovo tag> <tag base>` di `ziptv2`, clone
   parziale (`blob:none`). Le righe di versione le risolve il merge driver (sotto);
   ogni altro conflitto apre (o commenta) l'issue «Rebase di ziptv2 ... in conflitto».
4. **Test del solo plugin**: `flutter test` (Dart) e
   `gradlew :video_player_android:testDebugUnitTest` dall'esempio (Java), con la
   versione di Flutter letta dalla `ci.yml` di ziptv2. Niente integration test
   (serve un emulatore): li sostituisce la UAT on-device.
5. **Push** di `ziptv2-next` e del tag `ziptv2/<versione>-<nostro>`: il tag tiene
   il candidato raggiungibile anche dopo i push forzati successivi.
6. **PR in ziptv2** (branch `video-player-candidate/<versione>-<nostro>`):
   aggiorna `ref:` e `pubspec.lock`, riporta le novità del CHANGELOG upstream.
   analyze e test dell'app li fa la CI di ziptv2 sulla PR. Una PR nuova chiude
   quelle dei candidati precedenti.
7. **Merge** = promozione (prima: UAT su Fire Stick e Fold). **Rifiuto** = chiudere
   la PR: la stessa coppia tag/branch non viene riproposta.

**Segreto** `ZIPTV2_TOKEN`: fine-grained PAT su `ToRvaLDz/ziptv2` con Contents e
Pull requests in scrittura (stesso token del fork EPG).

## Merge driver delle versioni

Ogni tag upstream cambia `version:` e apre una sezione in cima al CHANGELOG; i
nostri commit `chore: versione X+ziptv2.N` toccano le stesse righe. `version_merge.py`
li risolve: versione `<nuovo tag>+ziptv2.N`, sezioni `## X+ziptv2.N` in cima,
rinominate sul nuovo tag. Il resto passa per `git merge-file`. Test:
`python3 .ziptv2/test_version_merge.py`.

Per un rebase a mano:

```sh
cp .ziptv2/version_merge.py /tmp/version_merge.py
git config merge.ziptv2-version.driver "python3 /tmp/version_merge.py %O %A %B %P"
printf '%s merge=ziptv2-version\n' \
  packages/video_player/video_player_android/pubspec.yaml \
  packages/video_player/video_player_android/CHANGELOG.md >> "$(git rev-parse --git-path info/attributes)"
git rebase --onto video_player_android-v<nuovo> video_player_android-v<base>
```

I workflow upstream sono disabilitati su questo fork.
