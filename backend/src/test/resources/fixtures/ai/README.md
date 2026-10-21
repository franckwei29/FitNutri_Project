# Synthetic image fixture

synthetic-2x2.webp is an owned38-byte2x2 solid RGB(24,128,56) lossless WebP, generated locally from a P6 PPM using the already installed official cwebp encoder. It contains no photo, person, food observation or EXIF/location. It tests actual WebP decoding/reencoding only; no model recognized this fixture.

Reproduce in a temporary directory:

```python
from pathlib import Path
Path('/private/tmp/fitnutri-synthetic-2x2.ppm').write_bytes(b'P6\n2 2\n255\n' + bytes([24,128,56]) * 4)
```

```sh
cwebp -lossless -quiet /private/tmp/fitnutri-synthetic-2x2.ppm -o synthetic-2x2.webp
```

JPEG/PNG/oversized-dimension fixtures are generated in memory by ImageValidatorTest. Its EXIF-like marker is synthetic and must disappear from the normalized output. No user's image or third-party copyrighted fixture is stored here.

# Synthetic audio fixtures (A03)

The three audio files are locally generated440Hz tones, not human speech, food/exercise evidence or a user's recording. They prove real container/codec validation only. Controlled provider text is explicitly synthetic and does not prove recognition/transcription quality or A10 live smoke. Local encoder/decoder tested: FFmpeg6.1.1.

```sh
ffmpeg -hide_banner -loglevel error -f lavfi -i sine=frequency=440:sample_rate=48000:duration=1 -c:a libopus -b:a 16k -metadata comment=SYNTHETIC_PRIVATE_METADATA -f webm synthetic-tone.webm
ffmpeg -hide_banner -loglevel error -f lavfi -i sine=frequency=440:sample_rate=48000:duration=1 -c:a aac -b:a 24k -metadata comment=SYNTHETIC_PRIVATE_METADATA -movflags frag_keyframe+empty_moov -f mp4 synthetic-tone.mp4
ffmpeg -hide_banner -loglevel error -f lavfi -i sine=frequency=440:sample_rate=48000:duration=61 -c:a libopus -b:a 8k -f webm synthetic-long-tone.webm
```

The valid files include a synthetic metadata marker which must be absent from normalized WAV/provider requests. The61second file must be rejected before any provider call. Tests never retain real microphone samples or audio in the database. A timeout test creates an owned executable script and PID file in JUnit's temporary directory; these are test process-control metadata, not saved audio.
