package net.runelite.cache.definitions;

import net.runelite.cache.models.CircularAngle;
import net.runelite.cache.models.FaceNormal;
import net.runelite.cache.models.VertexNormal;

public class ModelDefinition {
	public int id;

	public int vertexCount = 0;
	public int[] vertexPositionsX;
	public int[] vertexPositionsY;
	public int[] vertexPositionsZ;
	public transient VertexNormal[] vertexNormals;

	public int faceCount;
	public int[] faceVertexIndices1;
	public int[] faceVertexIndices2;
	public int[] faceVertexIndices3;
	public byte[] faceAlphas;
	public short[] faceColors;
	public byte[] faceRenderPriorities;
	public byte[] faceRenderTypes;
	public transient FaceNormal[] faceNormals;

	public int textureTriangleCount;
	public short[] textureTriangleVertexIndices1;
	public short[] textureTriangleVertexIndices2;
	public short[] textureTriangleVertexIndices3;
	public transient float[][] faceTextureUCoordinates;
	public transient float[][] faceTextureVCoordinates;
	public short[] texturePrimaryColors;
	public short[] faceTextures;
	public byte[] textureCoordinates;
	public byte[] textureRenderTypes;

	public int[] vertexSkins;
	public int[] faceSkins;

	public byte priority;

	public short[] aShortArray2574;
	public short[] aShortArray2575;
	public short[] aShortArray2577;
	public short[] aShortArray2578;
	public byte[] aByteArray2580;
	public short[] aShortArray2586;

	private int[][] vertexAnimationTable;
	private int[][] faceAnimationTable;
	private int mappedTranslatorX;
	private int mappedTranslatorY;
	private int mappedTranslatorZ;

	public ModelDefinition() {
	}

	public void computeNormals() {
		if (this.vertexNormals != null) {
			return;
		}

		this.vertexNormals = new VertexNormal[this.vertexCount];

		int var1;
		for (var1 = 0; var1 < this.vertexCount; ++var1) {
			this.vertexNormals[var1] = new VertexNormal();
		}

		for (var1 = 0; var1 < this.faceCount; ++var1) {
			int vertexA = this.faceVertexIndices1[var1];
			int vertexB = this.faceVertexIndices2[var1];
			int vertexC = this.faceVertexIndices3[var1];

			int xA = this.vertexPositionsX[vertexB] - this.vertexPositionsX[vertexA];
			int yA = this.vertexPositionsY[vertexB] - this.vertexPositionsY[vertexA];
			int zA = this.vertexPositionsZ[vertexB] - this.vertexPositionsZ[vertexA];

			int xB = this.vertexPositionsX[vertexC] - this.vertexPositionsX[vertexA];
			int yB = this.vertexPositionsY[vertexC] - this.vertexPositionsY[vertexA];
			int zB = this.vertexPositionsZ[vertexC] - this.vertexPositionsZ[vertexA];

			// Compute cross product
			int var11 = yA * zB - yB * zA;
			int var12 = zA * xB - zB * xA;
			int var13 = xA * yB - xB * yA;

			while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192) {
				var11 >>= 1;
				var12 >>= 1;
				var13 >>= 1;
			}

			int length = (int) Math.sqrt((double) (var11 * var11 + var12 * var12 + var13 * var13));
			if (length <= 0) {
				length = 1;
			}

			var11 = var11 * 256 / length;
			var12 = var12 * 256 / length;
			var13 = var13 * 256 / length;

			byte var15;
			if (this.faceRenderTypes == null) {
				var15 = 0;
			} else {
				var15 = this.faceRenderTypes[var1];
			}

			if (var15 == 0) {
				VertexNormal var16 = this.vertexNormals[vertexA];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexB];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexC];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;
			} else if (var15 == 1) {
				if (this.faceNormals == null) {
					this.faceNormals = new FaceNormal[this.faceCount];
				}

				FaceNormal var17 = this.faceNormals[var1] = new FaceNormal();
				var17.x = var11;
				var17.y = var12;
				var17.z = var13;
			}
		}
	}

	/**
	 * Computes the UV coordinates for every three-vertex face that has a
	 * texture.
	 */
	public void computeTextureUVCoordinates() {
		this.faceTextureUCoordinates = new float[faceCount][];
		this.faceTextureVCoordinates = new float[faceCount][];

		for (int i = 0; i < faceCount; i++) {
			int textureCoordinate;
			if (textureCoordinates == null) {
				textureCoordinate = -1;
			} else {
				textureCoordinate = textureCoordinates[i];
			}

			int textureIdx;
			if (faceTextures == null) {
				textureIdx = -1;
			} else {
				textureIdx = faceTextures[i] & 0xFFFF;
			}

			if (textureIdx != -1) {
				float[] u = new float[3];
				float[] v = new float[3];

				if (textureCoordinate == -1) {
					u[0] = 0.0F;
					v[0] = 1.0F;

					u[1] = 1.0F;
					v[1] = 1.0F;

					u[2] = 0.0F;
					v[2] = 0.0F;
				} else {
					textureCoordinate &= 0xFF;

					byte textureRenderType = 0;
					if (textureRenderTypes != null) {
						textureRenderType = textureRenderTypes[textureCoordinate];
					}

					if (textureRenderType == 0) {
						int faceVertexIdx1 = faceVertexIndices1[i];
						int faceVertexIdx2 = faceVertexIndices2[i];
						int faceVertexIdx3 = faceVertexIndices3[i];

						short triangleVertexIdx1 = textureTriangleVertexIndices1[textureCoordinate];
						short triangleVertexIdx2 = textureTriangleVertexIndices2[textureCoordinate];
						short triangleVertexIdx3 = textureTriangleVertexIndices3[textureCoordinate];

						float triangleX = (float) vertexPositionsX[triangleVertexIdx1];
						float triangleY = (float) vertexPositionsY[triangleVertexIdx1];
						float triangleZ = (float) vertexPositionsZ[triangleVertexIdx1];

						float f_882_ = (float) vertexPositionsX[triangleVertexIdx2] - triangleX;
						float f_883_ = (float) vertexPositionsY[triangleVertexIdx2] - triangleY;
						float f_884_ = (float) vertexPositionsZ[triangleVertexIdx2] - triangleZ;
						float f_885_ = (float) vertexPositionsX[triangleVertexIdx3] - triangleX;
						float f_886_ = (float) vertexPositionsY[triangleVertexIdx3] - triangleY;
						float f_887_ = (float) vertexPositionsZ[triangleVertexIdx3] - triangleZ;
						float f_888_ = (float) vertexPositionsX[faceVertexIdx1] - triangleX;
						float f_889_ = (float) vertexPositionsY[faceVertexIdx1] - triangleY;
						float f_890_ = (float) vertexPositionsZ[faceVertexIdx1] - triangleZ;
						float f_891_ = (float) vertexPositionsX[faceVertexIdx2] - triangleX;
						float f_892_ = (float) vertexPositionsY[faceVertexIdx2] - triangleY;
						float f_893_ = (float) vertexPositionsZ[faceVertexIdx2] - triangleZ;
						float f_894_ = (float) vertexPositionsX[faceVertexIdx3] - triangleX;
						float f_895_ = (float) vertexPositionsY[faceVertexIdx3] - triangleY;
						float f_896_ = (float) vertexPositionsZ[faceVertexIdx3] - triangleZ;

						float f_897_ = f_883_ * f_887_ - f_884_ * f_886_;
						float f_898_ = f_884_ * f_885_ - f_882_ * f_887_;
						float f_899_ = f_882_ * f_886_ - f_883_ * f_885_;
						float f_900_ = f_886_ * f_899_ - f_887_ * f_898_;
						float f_901_ = f_887_ * f_897_ - f_885_ * f_899_;
						float f_902_ = f_885_ * f_898_ - f_886_ * f_897_;
						float f_903_ = 1.0F / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_);

						u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
						u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
						u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

						f_900_ = f_883_ * f_899_ - f_884_ * f_898_;
						f_901_ = f_884_ * f_897_ - f_882_ * f_899_;
						f_902_ = f_882_ * f_898_ - f_883_ * f_897_;
						f_903_ = 1.0F / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_);

						v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
						v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
						v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;
					}
				}

				this.faceTextureUCoordinates[i] = u;
				this.faceTextureVCoordinates[i] = v;
			}
		}
	}

	public void rotate(int orientation) {
		int sin = CircularAngle.SINE[orientation];
		int cos = CircularAngle.COSINE[orientation];

		assert vertexPositionsX.length == vertexPositionsY.length;
		assert vertexPositionsY.length == vertexPositionsZ.length;

		for (int i = 0; i < vertexPositionsX.length; ++i) {
			vertexPositionsX[i] = vertexPositionsX[i] * cos + vertexPositionsZ[i] * sin >> 16;
			vertexPositionsZ[i] = vertexPositionsZ[i] * cos - vertexPositionsX[i] * sin >> 16;
		}

		reset();
	}

	public void invert() {
		int var1;
		for (var1 = 0; var1 < this.vertexCount; ++var1) {
			this.vertexPositionsZ[var1] = -this.vertexPositionsZ[var1];
		}

		for (var1 = 0; var1 < this.faceCount; ++var1) {
			int var2 = this.faceVertexIndices1[var1];
			this.faceVertexIndices1[var1] = this.faceVertexIndices3[var1];
			this.faceVertexIndices3[var1] = var2;
		}

		reset();
	}

	public void rotate1() {
		for (int var1 = 0; var1 < this.vertexCount; ++var1) {
			int var2 = this.vertexPositionsX[var1];
			this.vertexPositionsX[var1] = this.vertexPositionsZ[var1];
			this.vertexPositionsZ[var1] = -var2;
		}

		reset();
	}

	public void rotate2() {
		for (int var1 = 0; var1 < this.vertexCount; ++var1) {
			this.vertexPositionsX[var1] = -this.vertexPositionsX[var1];
			this.vertexPositionsZ[var1] = -this.vertexPositionsZ[var1];
		}

		reset();
	}

	public void rotate3() {
		for (int var1 = 0; var1 < this.vertexCount; ++var1) {
			int var2 = this.vertexPositionsZ[var1];
			this.vertexPositionsZ[var1] = this.vertexPositionsX[var1];
			this.vertexPositionsX[var1] = -var2;
		}

		reset();
	}

	public void rotate4() {
		for (int var1 = 0; var1 < this.vertexCount; ++var1) {
			int var2 = this.vertexPositionsY[var1];
			this.vertexPositionsY[var1] = -this.vertexPositionsZ[var1];
			this.vertexPositionsZ[var1] = var2;
		}

		reset();
	}

	private void reset() {
		vertexNormals = null;
		faceNormals = null;
		faceTextureUCoordinates = faceTextureVCoordinates = null;
	}

	public void resize(int var1, int var2, int var3) {
		for (int var4 = 0; var4 < this.vertexCount; ++var4) {
			this.vertexPositionsX[var4] = this.vertexPositionsX[var4] * var1 / 128;
			this.vertexPositionsY[var4] = var2 * this.vertexPositionsY[var4] / 128;
			this.vertexPositionsZ[var4] = var3 * this.vertexPositionsZ[var4] / 128;
		}

		reset();
	}

	public void recolor(short var1, short var2) {
		for (int var3 = 0; var3 < this.faceCount; ++var3) {
			if (this.faceColors[var3] == var1) {
				this.faceColors[var3] = var2;
			}
		}

	}

	public void retexture(short var1, short var2) {
		if (this.faceTextures != null) {
			for (int var3 = 0; var3 < this.faceCount; ++var3) {
				if (this.faceTextures[var3] == var1) {
					this.faceTextures[var3] = var2;
				}
			}

		}
	}

	// public void animate(FrameDefinition frame) {
	// int m = Integer.MIN_VALUE;
	// for(int i = 0; i < frame.field1310; i++) {
	// int frameId = frame.indexFrameIds[i];
	// int[] faces = frame.framemap.field1457[frameId];
	//
	// for(int j = 0; j < faces.length; j++) {
	// int faceIndex = faces[j];
	// m = Integer.max(m, frameId);
	//
	//// this.vertexPositionsX[faceIndex] += frame.translator_x[i];
	//// this.vertexPositionsY[faceIndex] += frame.translator_y[i];
	//// this.vertexPositionsZ[faceIndex] += frame.translator_z[i];
	// int vertexA = this.faceVertexIndices1[faceIndex];
	// int vertexB = this.faceVertexIndices2[faceIndex];
	// int vertexC = this.faceVertexIndices3[faceIndex];
	//
	// this.vertexPositionsX[vertexA] += frame.translator_x[i];
	// this.vertexPositionsY[vertexA] += frame.translator_y[i];
	// this.vertexPositionsZ[vertexA] += frame.translator_z[i];
	//
	// this.vertexPositionsX[vertexB] += frame.translator_x[i];
	// this.vertexPositionsY[vertexB] += frame.translator_y[i];
	// this.vertexPositionsZ[vertexB] += frame.translator_z[i];
	//
	// this.vertexPositionsX[vertexC] += frame.translator_x[i];
	// this.vertexPositionsY[vertexC] += frame.translator_y[i];
	// this.vertexPositionsZ[vertexC] += frame.translator_z[i];
	// }
	// }
	//
	// System.out.println("Max index: " + m);
	//
	// reset();
	// }

	public void animate(FrameDefinition frame) {
		FramemapDefinition framemap = frame.framemap;
		for (int i = 0; i < frame.field1310; ++i) {
			int frameIdx = frame.indexFrameIds[i];

			translate(framemap.field1456[frameIdx], framemap.field1457[frameIdx], frame.translator_x[i],
					frame.translator_y[i], frame.translator_z[i]);
		}

		reset();
		// if(this.field1857 != null) {
		// if(var2 != -1) {
		// Frame var3 = var1.skeletons[var2];
		// FrameMap var4 = var3.skin;
		// field1891 = 0;
		// field1892 = 0;
		// field1864 = 0;
		//
		// for(int var5 = 0; var5 < var3.field1803; ++var5) {
		// int var6 = var3.field1806[var5];
		// this.method2766(var4.types[var6], var4.list[var6],
		// var3.translator_x[var5], var3.translator_y[var5],
		// var3.translator_z[var5]);
		// }
		//
		// this.resetBounds();
		// }
		// }
	}

	private void translate(int type, int[] map, int translatorX, int translatorY, int translatorZ) {
		int var6 = map.length;
		int count;
		int animTableIdx;
		int var11;
		int var12;

		if (type == 0) {
			count = 0;
			mappedTranslatorX = 0;
			mappedTranslatorY = 0;
			mappedTranslatorZ = 0;

			for (int i = 0; i < var6; ++i) {
				int var9 = map[i];
				if (var9 < this.vertexAnimationTable.length) {
					int[] var10 = this.vertexAnimationTable[var9];

					for (var11 = 0; var11 < var10.length; ++var11) {
						var12 = var10[var11];
						mappedTranslatorX += this.vertexPositionsX[var12];
						mappedTranslatorY += this.vertexPositionsY[var12];
						mappedTranslatorZ += this.vertexPositionsZ[var12];
						++count;
					}
				}
			}

			if (count > 0) {
				mappedTranslatorX = translatorX + mappedTranslatorX / count;
				mappedTranslatorY = translatorY + mappedTranslatorY / count;
				mappedTranslatorZ = translatorZ + mappedTranslatorZ / count;
			} else {
				mappedTranslatorX = translatorX;
				mappedTranslatorY = translatorY;
				mappedTranslatorZ = translatorZ;
			}

		} else {
			int[] var18;
			int var19;
			if (type == 1) {
				for (count = 0; count < var6; ++count) {
					animTableIdx = map[count];
					if (animTableIdx < this.vertexAnimationTable.length) {
						var18 = this.vertexAnimationTable[animTableIdx];

						for (var19 = 0; var19 < var18.length; ++var19) {
							var11 = var18[var19];
							this.vertexPositionsX[var11] += translatorX;
							this.vertexPositionsY[var11] += translatorY;
							this.vertexPositionsZ[var11] += translatorZ;
						}
					}
				}

			} else if (type == 2) {
				for (count = 0; count < var6; ++count) {
					animTableIdx = map[count];
					if (animTableIdx < this.vertexAnimationTable.length) {
						var18 = this.vertexAnimationTable[animTableIdx];

						for (var19 = 0; var19 < var18.length; ++var19) {
							var11 = var18[var19];
							this.vertexPositionsX[var11] -= mappedTranslatorX;
							this.vertexPositionsY[var11] -= mappedTranslatorY;
							this.vertexPositionsZ[var11] -= mappedTranslatorZ;
							var12 = (translatorX & 255) * 8;
							int var13 = (translatorY & 255) * 8;
							int var14 = (translatorZ & 255) * 8;
							int var15;
							int var16;
							int var17;
							if (var14 != 0) {
								var15 = CircularAngle.SINE[var14];
								var16 = CircularAngle.COSINE[var14];
								var17 = var15 * this.vertexPositionsY[var11]
										+ var16 * this.vertexPositionsX[var11] >> 16;
								this.vertexPositionsY[var11] = var16 * this.vertexPositionsY[var11]
										- var15 * this.vertexPositionsX[var11] >> 16;
								this.vertexPositionsX[var11] = var17;
							}

							if (var12 != 0) {
								var15 = CircularAngle.SINE[var12];
								var16 = CircularAngle.COSINE[var12];
								var17 = var16 * this.vertexPositionsY[var11]
										- var15 * this.vertexPositionsZ[var11] >> 16;
								this.vertexPositionsZ[var11] = var15 * this.vertexPositionsY[var11]
										+ var16 * this.vertexPositionsZ[var11] >> 16;
								this.vertexPositionsY[var11] = var17;
							}

							if (var13 != 0) {
								var15 = CircularAngle.SINE[var13];
								var16 = CircularAngle.COSINE[var13];
								var17 = var15 * this.vertexPositionsZ[var11]
										+ var16 * this.vertexPositionsX[var11] >> 16;
								this.vertexPositionsZ[var11] = var16 * this.vertexPositionsZ[var11]
										- var15 * this.vertexPositionsX[var11] >> 16;
								this.vertexPositionsX[var11] = var17;
							}

							this.vertexPositionsX[var11] += mappedTranslatorX;
							this.vertexPositionsY[var11] += mappedTranslatorY;
							this.vertexPositionsZ[var11] += mappedTranslatorZ;
						}
					}
				}

			} else if (type == 3) {
				for (count = 0; count < var6; ++count) {
					animTableIdx = map[count];
					if (animTableIdx < this.vertexAnimationTable.length) {
						var18 = this.vertexAnimationTable[animTableIdx];

						for (var19 = 0; var19 < var18.length; ++var19) {
							var11 = var18[var19];
							this.vertexPositionsX[var11] -= mappedTranslatorX;
							this.vertexPositionsY[var11] -= mappedTranslatorY;
							this.vertexPositionsZ[var11] -= mappedTranslatorZ;
							this.vertexPositionsX[var11] = translatorX * this.vertexPositionsX[var11] / 128;
							this.vertexPositionsY[var11] = translatorY * this.vertexPositionsY[var11] / 128;
							this.vertexPositionsZ[var11] = translatorZ * this.vertexPositionsZ[var11] / 128;
							this.vertexPositionsX[var11] += mappedTranslatorX;
							this.vertexPositionsY[var11] += mappedTranslatorY;
							this.vertexPositionsZ[var11] += mappedTranslatorZ;
						}
					}
				}

			} else if (type == 5) {
				if (this.faceAnimationTable != null && this.faceAlphas != null) {
					for (count = 0; count < var6; ++count) {
						animTableIdx = map[count];
						if (animTableIdx < this.faceAnimationTable.length) {
							var18 = this.faceAnimationTable[animTableIdx];

							for (var19 = 0; var19 < var18.length; ++var19) {
								var11 = var18[var19];
								var12 = (this.faceAlphas[var11] & 255) + translatorX * 8;
								if (var12 < 0) {
									var12 = 0;
								} else if (var12 > 255) {
									var12 = 255;
								}

								this.faceAlphas[var11] = (byte) var12;
							}
						}
					}
				}

			}
		}
	}

	public void computeAnimationTables() {
		int[] var1;
		int var2;
		int var3;
		int var4;
		if (this.vertexSkins != null) {
			var1 = new int[256];
			var2 = 0;

			for (var3 = 0; var3 < this.vertexCount; ++var3) {
				var4 = this.vertexSkins[var3];
				++var1[var4];
				if (var4 > var2) {
					var2 = var4;
				}
			}

			this.vertexAnimationTable = new int[var2 + 1][];

			for (var3 = 0; var3 <= var2; ++var3) {
				this.vertexAnimationTable[var3] = new int[var1[var3]];
				var1[var3] = 0;
			}

			for (var3 = 0; var3 < this.vertexCount; this.vertexAnimationTable[var4][var1[var4]++] = var3++) {
				var4 = this.vertexSkins[var3];
			}

			this.vertexSkins = null;
		}

		if (this.faceSkins != null) {
			var1 = new int[256];
			var2 = 0;

			for (var3 = 0; var3 < this.faceCount; ++var3) {
				var4 = this.faceSkins[var3];
				++var1[var4];
				if (var4 > var2) {
					var2 = var4;
				}
			}

			this.faceAnimationTable = new int[var2 + 1][];

			for (var3 = 0; var3 <= var2; ++var3) {
				this.faceAnimationTable[var3] = new int[var1[var3]];
				var1[var3] = 0;
			}

			for (var3 = 0; var3 < this.faceCount; this.faceAnimationTable[var4][var1[var4]++] = var3++) {
				var4 = this.faceSkins[var3];
			}

			this.faceSkins = null;
		}

	}

	public ModelDefinition(ModelDefinition[] models, int var2) {
		this.id = 1337;
		this.vertexCount = 0;
		this.faceCount = 0;
		this.priority = 0;
		boolean var3 = false;
		boolean var4 = false;
		boolean var5 = false;
		boolean var6 = false;
		boolean var7 = false;
		boolean var8 = false;
		this.vertexCount = 0;
		this.faceCount = 0;
		this.textureTriangleCount = 0;
		this.priority = -1;

		int var9;
		ModelDefinition var10;
		for (var9 = 0; var9 < var2; ++var9) {
			var10 = models[var9];
			if (var10 != null) {
				this.vertexCount += var10.vertexCount;
				this.faceCount += var10.faceCount;
				this.textureTriangleCount += var10.textureTriangleCount;
				if (var10.faceRenderPriorities != null) {
					var4 = true;
				} else {
					if (this.priority == -1) {
						this.priority = var10.priority;
					}

					if (this.priority != var10.priority) {
						var4 = true;
					}
				}

				var3 |= var10.faceRenderTypes != null;
				var5 |= var10.faceAlphas != null;
				var6 |= var10.faceSkins != null;
				var7 |= var10.faceTextures != null;
				var8 |= var10.textureCoordinates != null;
			}
		}

		this.vertexPositionsX = new int[this.vertexCount];
		this.vertexPositionsY = new int[this.vertexCount];
		this.vertexPositionsZ = new int[this.vertexCount];
		this.vertexSkins = new int[this.vertexCount];
		this.faceVertexIndices1 = new int[this.faceCount];
		this.faceVertexIndices2 = new int[this.faceCount];
		this.faceVertexIndices3 = new int[this.faceCount];
		if (var3) {
			this.faceRenderTypes = new byte[this.faceCount];
		}

		if (var4) {
			this.faceRenderPriorities = new byte[this.faceCount];
		}

		if (var5) {
			this.faceAlphas = new byte[this.faceCount];
		}

		if (var6) {
			this.faceSkins = new int[this.faceCount];
		}

		if (var7) {
			this.faceTextures = new short[this.faceCount];
		}

		if (var8) {
			this.textureCoordinates = new byte[this.faceCount];
		}

		this.faceColors = new short[this.faceCount];
		if (this.textureTriangleCount > 0) {
			this.textureRenderTypes = new byte[this.textureTriangleCount];
			this.textureTriangleVertexIndices1 = new short[this.textureTriangleCount];
			this.textureTriangleVertexIndices2 = new short[this.textureTriangleCount];
			this.textureTriangleVertexIndices3 = new short[this.textureTriangleCount];
			this.aShortArray2574 = new short[this.textureTriangleCount];
			this.aShortArray2575 = new short[this.textureTriangleCount];
			this.aShortArray2586 = new short[this.textureTriangleCount];
			this.aShortArray2577 = new short[this.textureTriangleCount];
			this.aByteArray2580 = new byte[this.textureTriangleCount];
			this.aShortArray2578 = new short[this.textureTriangleCount];
			this.texturePrimaryColors = new short[this.textureTriangleCount];
		}

		this.vertexCount = 0;
		this.faceCount = 0;
		this.textureTriangleCount = 0;

		for (var9 = 0; var9 < var2; ++var9) {
			var10 = models[var9];
			if (var10 != null) {
				int var11;
				for (var11 = 0; var11 < var10.faceCount; ++var11) {
					if (var3 && var10.faceRenderTypes != null) {
						this.faceRenderTypes[this.faceCount] = var10.faceRenderTypes[var11];
					}

					if (var4) {
						if (var10.faceRenderPriorities != null) {
							this.faceRenderPriorities[this.faceCount] = var10.faceRenderPriorities[var11];
						} else {
							this.faceRenderPriorities[this.faceCount] = var10.priority;
						}
					}

					if (var5 && var10.faceAlphas != null) {
						this.faceAlphas[this.faceCount] = var10.faceAlphas[var11];
					}

					if (var6 && var10.faceSkins != null) {
						this.faceSkins[this.faceCount] = var10.faceSkins[var11];
					}

					if (var7) {
						if (var10.faceTextures != null) {
							this.faceTextures[this.faceCount] = var10.faceTextures[var11];
						} else {
							this.faceTextures[this.faceCount] = -1;
						}
					}

					if (var8) {
						if (var10.textureCoordinates != null && var10.textureCoordinates[var11] != -1) {
							this.textureCoordinates[this.faceCount] = (byte) (this.textureTriangleCount
									+ var10.textureCoordinates[var11]);
						} else {
							this.textureCoordinates[this.faceCount] = -1;
						}
					}

					this.faceColors[this.faceCount] = var10.faceColors[var11];
					this.faceVertexIndices1[this.faceCount] = this.adjustVertices(var10,
							var10.faceVertexIndices1[var11]);
					this.faceVertexIndices2[this.faceCount] = this.adjustVertices(var10,
							var10.faceVertexIndices2[var11]);
					this.faceVertexIndices3[this.faceCount] = this.adjustVertices(var10,
							var10.faceVertexIndices3[var11]);
					++this.faceCount;
				}

				for (var11 = 0; var11 < var10.textureTriangleCount; ++var11) {
					byte var12 = this.textureRenderTypes[this.textureTriangleCount] = var10.textureRenderTypes[var11];
					if (var12 == 0) {
						this.textureTriangleVertexIndices1[this.textureTriangleCount] = (short) this
								.adjustVertices(var10, var10.textureTriangleVertexIndices1[var11]);
						this.textureTriangleVertexIndices2[this.textureTriangleCount] = (short) this
								.adjustVertices(var10, var10.textureTriangleVertexIndices2[var11]);
						this.textureTriangleVertexIndices3[this.textureTriangleCount] = (short) this
								.adjustVertices(var10, var10.textureTriangleVertexIndices3[var11]);
					}

					if (var12 >= 1 && var12 <= 3) {
						this.textureTriangleVertexIndices1[this.textureTriangleCount] = var10.textureTriangleVertexIndices1[var11];
						this.textureTriangleVertexIndices2[this.textureTriangleCount] = var10.textureTriangleVertexIndices2[var11];
						this.textureTriangleVertexIndices3[this.textureTriangleCount] = var10.textureTriangleVertexIndices3[var11];
						this.aShortArray2574[this.textureTriangleCount] = var10.aShortArray2574[var11];
						this.aShortArray2575[this.textureTriangleCount] = var10.aShortArray2575[var11];
						this.aShortArray2586[this.textureTriangleCount] = var10.aShortArray2586[var11];
						this.aShortArray2577[this.textureTriangleCount] = var10.aShortArray2577[var11];
						this.aByteArray2580[this.textureTriangleCount] = var10.aByteArray2580[var11];
						this.aShortArray2578[this.textureTriangleCount] = var10.aShortArray2578[var11];
					}

					if (var12 == 2) {
						this.texturePrimaryColors[this.textureTriangleCount] = var10.texturePrimaryColors[var11];
					}

					++this.textureTriangleCount;
				}
			}
		}

	}

	final int adjustVertices(ModelDefinition model, int idx) {
		int var3 = -1;
		int var4 = model.vertexPositionsX[idx];
		int var5 = model.vertexPositionsY[idx];
		int var6 = model.vertexPositionsZ[idx];

		for (int var7 = 0; var7 < this.vertexCount; ++var7) {
			if (var4 == this.vertexPositionsX[var7] && var5 == this.vertexPositionsY[var7]
					&& var6 == this.vertexPositionsZ[var7]) {
				var3 = var7;
				break;
			}
		}

		if (var3 == -1) {
			this.vertexPositionsX[this.vertexCount] = var4;
			this.vertexPositionsY[this.vertexCount] = var5;
			this.vertexPositionsZ[this.vertexCount] = var6;
			if (model.vertexSkins != null) {
				this.vertexSkins[this.vertexCount] = model.vertexSkins[idx];
			}

			var3 = this.vertexCount++;
		}

		return var3;
	}
}