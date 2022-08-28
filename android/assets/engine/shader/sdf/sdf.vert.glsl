#include <engine/shader/header/spritebatch.vert>

//PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_channel"),
//PVertexAttributes.Attribute.genGenericAttribute("a_threshold", 1),
//PVertexAttributes.Attribute.genGenericAttribute("a_borderThresholdInwardsOffset", 1),
//PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_baseColor"),
//PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_borderColor"),

in vec4 a_channel;
out vec4 v_channel;

in float a_threshold;
out float v_threshold;

in float a_borderThresholdInwardsOffset;
out float v_borderThresholdInwardsOffset;

in vec4 a_baseColor;
out vec4 v_baseColor;

in vec4 a_borderColor;
out vec4 v_borderColor;

void main() {
    #include <engine/shader/start/spritebatch.vert>
v_channel = a_channel;
    v_threshold = a_threshold;
    v_borderThresholdInwardsOffset = a_borderThresholdInwardsOffset;
    v_baseColor = a_baseColor;
    v_borderColor = a_borderColor;
    #include <engine/shader/end/spritebatch.vert>
}