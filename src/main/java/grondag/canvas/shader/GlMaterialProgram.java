package grondag.canvas.shader;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.material.property.MaterialMatrixState;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.frex.api.material.UniformRefreshFrequency;

public class GlMaterialProgram extends GlProgram {
	// UGLY: special casing, public
	public final Uniform3fImpl modelOrigin;
	// converts world normals to normals of incoming vertex data
	public final UniformMatrix3fImpl normalModelMatrix;
	public final UniformArrayfImpl materialArray;
	public final Uniform3iImpl programInfo;
	public final Uniform1iImpl modelOriginType;
	public final Uniform1iImpl fogMode;

	GlMaterialProgram(Shader vertexShader, Shader fragmentShader, CanvasVertexFormat format, ProgramType programType) {
		super(vertexShader, fragmentShader, format, programType);
		modelOrigin = (Uniform3fImpl) uniform3f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0, 0));
		normalModelMatrix = uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.ON_LOAD, u -> {});
		materialArray = (UniformArrayfImpl) uniformArrayf("_cvu_material", UniformRefreshFrequency.ON_LOAD, u -> {}, 4);
		programInfo = (Uniform3iImpl) uniform3i("_cvu_program", UniformRefreshFrequency.ON_LOAD, u -> {});
		modelOriginType = (Uniform1iImpl) uniform1i("_cvu_model_origin_type", UniformRefreshFrequency.ON_LOAD, u -> u.set(MaterialMatrixState.getModelOrigin().ordinal()));
		fogMode = (Uniform1iImpl) uniform1i("_cvu_fog_mode", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
	}

	public void setModelOrigin(int x, int y, int z) {
		modelOrigin.set(x, y, z);
		modelOrigin.upload();
	}

	private final float[] materialData = new float[4];

	private static final int _CV_SPRITE_INFO_TEXTURE_SIZE = 0;
	private static final int _CV_ATLAS_WIDTH = 1;
	private static final int _CV_ATLAS_HEIGHT = 2;

	public void setAtlasInfo(SpriteInfoTexture atlasInfo) {
		if (atlasInfo == null) {
			materialData[_CV_SPRITE_INFO_TEXTURE_SIZE] = 0;
		} else {
			materialData[_CV_SPRITE_INFO_TEXTURE_SIZE] = atlasInfo.textureSize();
			materialData[_CV_ATLAS_WIDTH] = atlasInfo.atlasWidth();
			materialData[_CV_ATLAS_HEIGHT] = atlasInfo.atlasHeight();
		}

		materialArray.set(materialData);
		materialArray.upload();
	}
}
