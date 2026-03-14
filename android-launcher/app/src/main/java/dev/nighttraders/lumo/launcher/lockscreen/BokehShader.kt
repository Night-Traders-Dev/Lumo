package dev.nighttraders.lumo.launcher.lockscreen

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * GPU-accelerated Ubuntu Touch-style bokeh effect using AGSL.
 * Requires API 33+.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object BokehShader {

    // AGSL shader — no arrays, fully unrolled for compatibility
    private const val SHADER_SOURCE = """
        uniform float2 iResolution;
        uniform float  iTime;

        half3 palette(int idx) {
            int c = idx - (idx / 5) * 5;
            if (c == 0) return half3(0.914, 0.329, 0.125);
            if (c == 1) return half3(0.867, 0.282, 0.078);
            if (c == 2) return half3(0.812, 0.216, 0.129);
            if (c == 3) return half3(0.722, 0.188, 0.094);
            return half3(0.929, 0.463, 0.302);
        }

        half4 circle(float2 uv, float2 ctr, float r, float a, half3 col) {
            float d = length(uv - ctr);
            float glowR = r * 1.5;
            half glow = half(max(0.0, 1.0 - d / glowR));
            glow = glow * glow * half(a) * 0.25;
            half m = half(max(0.0, 1.0 - d / r));
            m = m * m * half(a);
            return half4(col * (m + glow), m + glow);
        }

        float2 orbit(float2 ctr, float hm, float deg, float frac, float period) {
            float ang = radians(deg) + iTime * 6.2832 / period;
            return ctr + float2(cos(ang), sin(ang)) * hm * frac;
        }

        half4 main(float2 fc) {
            float2 c = iResolution * 0.5;
            float hm = min(iResolution.x, iResolution.y) * 0.5;

            float yn = fc.y / iResolution.y;
            half3 bg = mix(half3(0.173, 0.0, 0.118), half3(0.369, 0.153, 0.314), half(smoothstep(0.0, 0.5, yn)));
            bg = mix(bg, half3(0.667, 0.224, 0.149), half(smoothstep(0.4, 0.7, yn)));
            bg = mix(bg, half3(0.173, 0.0, 0.118), half(smoothstep(0.7, 1.0, yn)));
            half3 r = bg;

            r += circle(fc, orbit(c,hm,  0.0,0.32,40.0), hm*0.22, 0.25, palette(0)).rgb;
            r += circle(fc, orbit(c,hm, 72.0,0.30,44.0), hm*0.20, 0.22, palette(1)).rgb;
            r += circle(fc, orbit(c,hm,144.0,0.34,38.0), hm*0.24, 0.28, palette(2)).rgb;
            r += circle(fc, orbit(c,hm,216.0,0.31,46.0), hm*0.21, 0.20, palette(3)).rgb;
            r += circle(fc, orbit(c,hm,288.0,0.33,42.0), hm*0.23, 0.26, palette(4)).rgb;

            r += circle(fc, orbit(c,hm, 20.0,0.45,50.0), hm*0.30, 0.30, palette(5)).rgb;
            r += circle(fc, orbit(c,hm, 65.0,0.48,55.0), hm*0.34, 0.28, palette(6)).rgb;
            r += circle(fc, orbit(c,hm,110.0,0.44,48.0), hm*0.28, 0.25, palette(7)).rgb;
            r += circle(fc, orbit(c,hm,155.0,0.50,52.0), hm*0.36, 0.32, palette(8)).rgb;
            r += circle(fc, orbit(c,hm,200.0,0.46,56.0), hm*0.32, 0.27, palette(9)).rgb;
            r += circle(fc, orbit(c,hm,245.0,0.43,54.0), hm*0.26, 0.22, palette(10)).rgb;
            r += circle(fc, orbit(c,hm,290.0,0.49,50.0), hm*0.34, 0.30, palette(11)).rgb;
            r += circle(fc, orbit(c,hm,335.0,0.47,58.0), hm*0.30, 0.26, palette(12)).rgb;

            r += circle(fc, orbit(c,hm, 30.0,0.62,65.0), hm*0.28, 0.20, palette(13)).rgb;
            r += circle(fc, orbit(c,hm, 90.0,0.66,70.0), hm*0.30, 0.18, palette(14)).rgb;
            r += circle(fc, orbit(c,hm,150.0,0.60,62.0), hm*0.26, 0.16, palette(15)).rgb;
            r += circle(fc, orbit(c,hm,210.0,0.64,68.0), hm*0.28, 0.14, palette(16)).rgb;
            r += circle(fc, orbit(c,hm,270.0,0.58,72.0), hm*0.24, 0.15, palette(17)).rgb;
            r += circle(fc, orbit(c,hm,330.0,0.63,66.0), hm*0.27, 0.17, palette(18)).rgb;

            return half4(r, 1.0);
        }
    """

    fun create(): RuntimeShader = RuntimeShader(SHADER_SOURCE)
}
