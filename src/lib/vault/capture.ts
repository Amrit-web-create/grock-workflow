export async function captureIntruder(): Promise<{ image: string | null; note: string | null }> {
  if (typeof navigator === "undefined" || !navigator.mediaDevices?.getUserMedia) {
    return { image: null, note: "This browser has no camera API. The attempt was logged without a photo." };
  }
  let stream: MediaStream | null = null;
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user", width: { ideal: 320 }, height: { ideal: 240 } },
      audio: false,
    });
    const video = document.createElement("video");
    video.playsInline = true;
    video.muted = true;
    video.srcObject = stream;
    await video.play();
    await new Promise<void>((resolve) => {
      if (video.videoWidth > 0) resolve();
      else video.onloadeddata = () => resolve();
    });
    const canvas = document.createElement("canvas");
    canvas.width = 160;
    canvas.height = 120;
    const ctx = canvas.getContext("2d");
    if (!ctx) return { image: null, note: "Could not open a canvas. The attempt was logged without a photo." };
    ctx.drawImage(video, 0, 0, 160, 120);
    const image = canvas.toDataURL("image/jpeg", 0.62);
    return { image, note: null };
  } catch {
    return {
      image: null,
      note: "Camera permission was blocked or unavailable. The attempt was logged without a photo.",
    };
  } finally {
    stream?.getTracks().forEach((track) => track.stop());
  }
}
