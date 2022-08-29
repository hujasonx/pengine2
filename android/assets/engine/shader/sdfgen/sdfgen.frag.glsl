#params [OUTPUT0]

#include <engine/shader/header/spritebatch.frag>
uniform float u_sheetPadding;
uniform vec4 u_inputUVOS;
uniform vec4 u_sheetPixelXYWH;
uniform float u_scale;


// The thickness of the gradient edge is dependent on the sheet padding.

float litStatusOfSourceUV(vec2 uv) {
    return step(2.9, dot(vec3(1.0), texture0Tex(uv).rgb));
}

float outputLevelForSourceUV(vec2 sourceUV) {
    vec2 sourceRegionPixelCorner = u_texture0TexSize.xy * u_inputUVOS.xy;
    vec2 sourceRegionPixelSize = u_texture0TexSize.xy * u_inputUVOS.zw;
    vec2 sourceRegionPixelCorner2 = sourceRegionPixelCorner + sourceRegionPixelSize;

    float sheetPixelIsPartOfPadding = 0.0;
    if (sourceUV.x < u_inputUVOS.x || sourceUV.x > u_inputUVOS.x + u_inputUVOS.z) {
        sheetPixelIsPartOfPadding = 1.0;
    }
    if (sourceUV.y < u_inputUVOS.y || sourceUV.y > u_inputUVOS.y + u_inputUVOS.w) {
        sheetPixelIsPartOfPadding = 1.0;
    }
    vec2 sourcePixel = sourceUV * u_texture0TexSize.xy;

    // The source texture must be all white to be considerered lit.
    float litStatus = litStatusOfSourceUV(sourceUV);// step(2.9, dot(vec3(1.0), texture0Tex(sheetPixelSourceUV).rgb));
    float minPixelDistanceToNearestOppositeLitStatus = 100000;
    // The number of pixels distant in the source texture to go from 0 to 0.5.
    // The padding has an extra pixel that shouldn't be used.
    float sourcePixelHalfDistance = (u_sheetPadding - 1.0) / u_scale;

    // Find the nearest pixel of the opposite color.
    for (float sourcePixelX = sourceRegionPixelCorner.x; sourcePixelX < sourceRegionPixelCorner2.x; sourcePixelX += 1.0) {
        for (float sourcePixelY = sourceRegionPixelCorner.y; sourcePixelY < sourceRegionPixelCorner2.y; sourcePixelY += 1.0) {
            vec2 checkSourcePixel = vec2(sourcePixelX, sourcePixelY);
            vec2 checkSourceUV = checkSourcePixel * u_texture0TexSize.zw;

            float checkLitStatus = step(2.9, dot(vec3(1.0), texture0Tex(checkSourceUV).rgb));
            if (abs(checkLitStatus - litStatus) > .5) {
                // The lit status is different.
                float distanceToSheetPixelSourcePixel = length(sourcePixel - checkSourcePixel);
                minPixelDistanceToNearestOppositeLitStatus = min(minPixelDistanceToNearestOppositeLitStatus, distanceToSheetPixelSourcePixel);
            }
        }
    }
    float scaledPixelDistance = minPixelDistanceToNearestOppositeLitStatus / sourcePixelHalfDistance;

    // Either -1 or 1, depending on the lit status (0 or 1).
    float litSign = 2.0 * (litStatus - 0.5);
    return 0.5 + litSign * scaledPixelDistance;
}

void main() {
    #include <engine/shader/start/spritebatch.frag>

    vec2 sourceRegionPixelCorner = u_texture0TexSize.xy * u_inputUVOS.xy;
    vec2 sourceRegionPixelSize = u_texture0TexSize.xy * u_inputUVOS.zw;
    vec2 sourceRegionPixelCorner2 = sourceRegionPixelCorner + sourceRegionPixelSize;

    // Includes padding.
    vec2 sheetPixelInSymbol = gl_FragCoord.xy - u_sheetPixelXYWH.xy;
    // Shifted by the padding.
    vec2 sheetPixelInSymbolPadded = gl_FragCoord.xy - u_sheetPixelXYWH.xy - vec2(u_sheetPadding, u_sheetPadding);
    // The pixel offset in the source texture from the symbol corner.
    vec2 sourcePixelInSymbolPadded = sheetPixelInSymbolPadded / u_scale;
    // The number of pixels distant in the source texture to go from 0 to 0.5.
    // The padding has an extra pixel that shouldn't be used.
    float sourcePixelHalfDistance = (u_sheetPadding - 1.0) / u_scale;
    // The pixel in the source texture.
    vec2 sheetPixelSourcePixel = sourceRegionPixelCorner + sourcePixelInSymbolPadded;

    vec2 sheetPixelSourceUV = sheetPixelSourcePixel * u_texture0TexSize.zw;

    float sheetPixelIsPartOfPadding = 0.0;
    if (sheetPixelSourceUV.x < u_inputUVOS.x || sheetPixelSourceUV.x > u_inputUVOS.x + u_inputUVOS.z) {
        sheetPixelIsPartOfPadding = 1.0;
    }
    if (sheetPixelSourceUV.y < u_inputUVOS.y || sheetPixelSourceUV.y > u_inputUVOS.y + u_inputUVOS.w) {
        sheetPixelIsPartOfPadding = 1.0;
    }

    //    // The source texture must be all white to be considerered lit.
    //    float litStatus = litStatusOfSourceUV(sheetPixelSourceUV);// step(2.9, dot(vec3(1.0), texture0Tex(sheetPixelSourceUV).rgb));
    //    float minPixelDistanceToNearestOppositeLitStatus = 100000;
    //
    //    // Find the nearest pixel of the opposite color.
    //    for (float sourcePixelX = sourceRegionPixelCorner.x; sourcePixelX < sourceRegionPixelCorner2.x; sourcePixelX += 1.0) {
    //        for (float sourcePixelY = sourceRegionPixelCorner.y; sourcePixelY < sourceRegionPixelCorner2.y; sourcePixelY += 1.0) {
    //            vec2 checkSourcePixel = vec2(sourcePixelX, sourcePixelY);
    //            vec2 checkSourceUV = checkSourcePixel * u_texture0TexSize.zw;
    //
    //            float checkLitStatus = step(2.9, dot(vec3(1.0), texture0Tex(checkSourceUV).rgb));
    //            if (abs(checkLitStatus - litStatus) > .5) {
    //                // The lit status is different.
    //                float distanceToSheetPixelSourcePixel = length(sheetPixelSourcePixel - checkSourcePixel);
    //                minPixelDistanceToNearestOppositeLitStatus = min(minPixelDistanceToNearestOppositeLitStatus, distanceToSheetPixelSourcePixel);
    //            }
    //        }
    //    }
    //    float scaledPixelDistance = minPixelDistanceToNearestOppositeLitStatus / sourcePixelHalfDistance;


    // Fill the output based on the minimum distance found.
    // Either -1 or 1, depending on the lit status (0 or 1).
//    float litSign = 2.0 * (litStatus - 0.5);
    //    float outputLevel = 0.5 + litSign * scaledPixelDistance;
    float outputLevel = outputLevelForSourceUV(sheetPixelSourceUV);
    OUTPUT0 = v_col0 * vec4(outputLevel);


    //    OUTPUT0 = vec4(v_col0) * texture0Tex(vec2(v_uv0.x, v_uv0.y));
    //    OUTPUT0.rg = vec2(litStatus);
    //    OUTPUT0.a = 1.0;
    //    OUTPUT0.xy = pixelForSymbol / u_sheetPixelXYWH.zw;


    //    OUTPUT0.g = litStatus;
    //    OUTPUT0.b = 0.5 + litSign * scaledPixelDistance;
    //    OUTPUT0.a = 1.0;
    //    OUTPUT0.g = texture0Tex(sheetPixelSourceUV).g;
    //    OUTPUT0.r = litStatus;
    //    OUTPUT0 =  vec4(0.0, 0.0, 0.0, 1.0);
    //    OUTPUT0.r = texture0Tex(sheetPixelSourceUV).r;
    //    OUTPUT0.gb = sheetPixelInSymbolPadded / (u_sheetPixelXYWH.zw - 2.8 * vec2(u_sheetPadding));
    //    if (OUTPUT0.g < 0.0 || OUTPUT0.g > 1.0) {
    //        OUTPUT0.r += 0.3;
    //    }
    //    if (OUTPUT0.b < 0.0 || OUTPUT0.b > 1.0) {
    //        OUTPUT0.r += 0.3;
    //    }

    if (sheetPixelIsPartOfPadding > 0.5) {
        OUTPUT0.a = 1.0;
        OUTPUT0.r = 1.0;
    }

        #include <engine/shader/end/spritebatch.frag>
}
