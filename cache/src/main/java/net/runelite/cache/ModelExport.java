package net.runelite.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.io.Files;

import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.SpotAnimDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.definitions.loaders.SpotAnimLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.models.FaceNormal;
import net.runelite.cache.models.ObjExporter;
import net.runelite.cache.models.VertexNormal;

public class ModelExport
{
	private static int sizeX = 128;
	private static int sizeY = 128;
	public static void main(String[] args) throws IOException
	{
		Options options = new Options();

		options.addOption("c", "cache", true, "cache base");

		options.addOption(null, "item", true, "directory to dump items to");
		options.addOption(null, "npc", true, "directory to dump npcs to");
		options.addOption(null, "object", true, "directory to dump objects to");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try
		{
			cmd = parser.parse(options, args);
		}
		catch (ParseException ex)
		{
			System.err.println("Error parsing command line options: " + ex.getMessage());
			System.exit(-1);
			return;
		}

		String cache = cmd.getOptionValue("cache");

		Store store = loadStore(cache);
		
		TextureManager tm = new TextureManager(store);
		tm.load();

		if (cmd.hasOption("item"))
		{
			Integer itemId = Integer.valueOf(cmd.getOptionValue("item"));

			if (itemId == null)
			{
				System.err.println("Item name must be specified");
				return;
			}
			
			ItemManager dumper = new ItemManager(store);
			dumper.load();
			ItemDefinition def = dumper.getItem(itemId);
			
			exportModels(store, def.name, null, def.inventoryModel, def.femaleModel0, def.maleModel0);
		} else if (cmd.hasOption("npc")) {
			NpcManager dumper = new NpcManager(store);
			dumper.load();
			
			String npcVal = cmd.getOptionValue("npc");
			Integer npcId = null;
			try {
				npcId = Integer.valueOf(npcVal);
			} catch (NumberFormatException e) {
				for(NpcDefinition npc : dumper.getNpcs()) {
					if(npc.name.equalsIgnoreCase(npcVal)) {
						if(npcId != null) {
							System.out.println("Duplicate NPC found: " + npc.id);
						} else {
							System.out.println("NPC found: " + npc.id);
							npcId = npc.id;
						}
					}
				}
			}
			
//			NPC found: 8026
//			Duplicate NPC found: 8058
//			Duplicate NPC found: 8059
//			Duplicate NPC found: 8060
//			Duplicate NPC found: 8061

			if (npcId == null)
			{
				System.err.println("NPC name must be specified");
				return;
			}
			
			
			NpcDefinition def = dumper.getNpc(npcId);
			sizeX = def.resizeX;
			sizeY = def.resizeY;
			int anim = def.stanceAnimation;
			
//			getFrame(store, anim);
			
			FrameDefinition frame = null;
			if(anim > -1) {
				SequenceDefinition sequence = getAnim(store, anim);//135004182
				
				frame = getFrame(store, sequence.frameIDs[0]);
			}
			
			
			exportModels(store, def.name, frame, def.models);
			exportModels(store, def.name, null, def.models_2);
		} else if (cmd.hasOption("object")) {
			ObjectManager dumper = new ObjectManager(store);
			dumper.load();
			
			String objectVal = cmd.getOptionValue("object");
			Integer objectId = null;
			try {
				objectId = Integer.valueOf(objectVal);
			} catch (NumberFormatException e) {
				for(ObjectDefinition object : dumper.getObjects()) {
					if(object.getName().contains(objectVal)) {
						if(objectId != null) {
							System.out.println("Duplicate Object found: " + object.getName() + " - " + object.getId());
						} else {
							System.out.println("Object found: " + object.getName() + " - " + object.getId());
							objectId = object.getId();
						}
					}
				}
			}
			
//			Object found: 31912
//			Duplicate Object found: 31977
//			Duplicate Object found: 31978
//			Duplicate Object found: 31985
			
//			NPC found: 8026
//			Duplicate NPC found: 8058
//			Duplicate NPC found: 8059
//			Duplicate NPC found: 8060
//			Duplicate NPC found: 8061

			if (objectId == null)
			{
				System.err.println("Object name must be specified");
				return;
			}
			
			
			ObjectDefinition def = dumper.getObject(objectId);
			
			exportModels(store, def.getName(), null, def.getObjectModels());
		}
	}
	
	
	
	private static SequenceDefinition getAnim(Store store, int animId) throws IOException {
		Index index = store.getIndex(IndexType.CONFIGS);
		Storage storage = store.getStorage();
		Archive archive = index.getArchive(ConfigType.SEQUENCE.getId());

		byte[] archiveData = storage.loadArchive(archive);
		ArchiveFiles files = archive.getFiles(archiveData);
		SequenceLoader loader = new SequenceLoader();
		for(FSFile f : files.getFiles()) {
			SequenceDefinition anim = loader.load(f.getFileId(), f.getContents());
			if(anim.getId() == animId)
				return anim;
		}
		
		return null;
	}
	
	private static FrameDefinition getFrame(Store store, int frameId) throws IOException {
		Storage storage = store.getStorage();
		Index frameIndex = store.getIndex(IndexType.FRAMES);
		
		FramemapManager fmManager = new FramemapManager(store);
		fmManager.load();
		FrameLoader frameLoader = new FrameLoader();
		

		for (Archive archive : frameIndex.getArchives())
		{
			byte[] archiveData = storage.loadArchive(archive);
			ArchiveFiles files = archive.getFiles(archiveData);
			for(FSFile f : files.getFiles()) {
				
				byte[] contents = f.getContents();
				
				int framemapArchiveId = (contents[0] & 0xff) << 8 | contents[1] & 0xff;

				FramemapDefinition framemap = fmManager.getNpc(framemapArchiveId);
									
				int fid = archive.getArchiveId() << 16 | f.getFileId();
				if(frameId == fid) 
					return frameLoader.load(frameId, framemap, contents);
			}
		}
		
		return null;
	}
	
	private static Store loadStore(String cache) throws IOException
	{
		Store store = new Store(new File(cache));
		store.load();
		return store;
	}
	
	private static void exportModels(Store store, String tag, FrameDefinition frame, int...modelIds) throws IOException {
		if(modelIds == null)
			return;
		
//		exportModel(store, frame, mergeModels(store, modelIds));
		
		for(int id : modelIds) {
			exportModel(store, frame, id, String.valueOf(id));
		}
	}
	
	private static ModelDefinition mergeModels(Store store, int[] modelIds) throws IOException {
		ModelDefinition merged = new ModelDefinition();
		merged.faceCount = 0;
		merged.textureTriangleCount = 0;
		merged.vertexCount = 0;
		
		ModelDefinition[] models = new ModelDefinition[modelIds.length];
		for(int i = 0; i < modelIds.length; i++) {
			ModelDefinition model = getModel(store, modelIds[i]);
			models[i] = model;
			merged.faceCount += model.faceCount;
			merged.textureTriangleCount += model.textureTriangleCount;
			merged.vertexCount += model.vertexCount;
		}
		
		merged.vertexPositionsX = new int[merged.vertexCount];
		merged.vertexPositionsY = new int[merged.vertexCount];
		merged.vertexPositionsZ = new int[merged.vertexCount];
		
		merged.faceVertexIndices1 = new int[merged.faceCount];
		merged.faceVertexIndices2 = new int[merged.faceCount];
		merged.faceVertexIndices3 = new int[merged.faceCount];
		merged.faceAlphas = new byte[merged.faceCount];
		merged.faceColors = new short[merged.faceCount];
		merged.faceRenderPriorities = new byte[merged.faceCount];
		merged.faceRenderTypes = new byte[merged.faceCount];
		
		if(merged.textureTriangleCount > 0) {
			merged.textureTriangleVertexIndices1 = new short[merged.textureTriangleCount];
			merged.textureTriangleVertexIndices2 = new short[merged.textureTriangleCount];
			merged.textureTriangleVertexIndices3 = new short[merged.textureTriangleCount];
			merged.texturePrimaryColors = new short[merged.textureTriangleCount];
			merged.faceTextures = new short[merged.textureTriangleCount];
			merged.textureCoordinates = new byte[merged.textureTriangleCount];
			merged.textureRenderTypes = new byte[merged.textureTriangleCount];
		}
		
		merged.vertexSkins = new int[merged.vertexCount];
		merged.faceSkins = new int[merged.faceCount];
		
		int vPos = 0;
		int fPos = 0;
		int tPos = 0;
		for(ModelDefinition model : models) {
			if(vPos == 0)
				merged.id = model.id;
			System.arraycopy(model.vertexPositionsX, 0, merged.vertexPositionsX, vPos, model.vertexPositionsX.length);
			System.arraycopy(model.vertexPositionsY, 0, merged.vertexPositionsY, vPos, model.vertexPositionsY.length);
			System.arraycopy(model.vertexPositionsZ, 0, merged.vertexPositionsZ, vPos, model.vertexPositionsZ.length);
			System.arraycopy(model.vertexSkins, 0, merged.vertexSkins, vPos, model.vertexSkins.length);
			vPos += model.vertexCount;
			
			System.arraycopy(model.faceVertexIndices1, 0, merged.faceVertexIndices1, fPos, model.faceVertexIndices1.length);
			System.arraycopy(model.faceVertexIndices2, 0, merged.faceVertexIndices2, fPos, model.faceVertexIndices2.length);
			System.arraycopy(model.faceVertexIndices3, 0, merged.faceVertexIndices3, fPos, model.faceVertexIndices3.length);
			if(model.faceAlphas != null)
				System.arraycopy(model.faceAlphas, 0, merged.faceAlphas, fPos, model.faceAlphas.length);
			else
				Arrays.fill(merged.faceAlphas, fPos, fPos + model.faceCount, (byte) 0);
			System.arraycopy(model.faceColors, 0, merged.faceColors, fPos, model.faceColors.length);
			if(model.faceRenderPriorities != null)
				System.arraycopy(model.faceRenderPriorities, 0, merged.faceRenderPriorities, fPos, model.faceRenderPriorities.length);
			else
				Arrays.fill(merged.faceRenderPriorities, fPos, fPos + model.faceCount, (byte) 0);
			if(model.faceRenderTypes != null)
				System.arraycopy(model.faceRenderTypes, 0, merged.faceRenderTypes, fPos, model.faceRenderTypes.length);
			else
				Arrays.fill(merged.faceRenderTypes, fPos, fPos + model.faceCount, (byte) 0);
			System.arraycopy(model.faceSkins, 0, merged.faceSkins, fPos, model.faceSkins.length);
			fPos += model.faceCount;
			
			if(merged.textureTriangleCount > 0) {
				if(model.textureTriangleVertexIndices1 != null)
					System.arraycopy(model.textureTriangleVertexIndices1, 0, merged.textureTriangleVertexIndices1, tPos, model.textureTriangleVertexIndices1.length);
				else
					Arrays.fill(merged.textureTriangleVertexIndices1, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.textureTriangleVertexIndices2 != null)
					System.arraycopy(model.textureTriangleVertexIndices2, 0, merged.textureTriangleVertexIndices2, tPos, model.textureTriangleVertexIndices2.length);
				else
					Arrays.fill(merged.textureTriangleVertexIndices2, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.textureTriangleVertexIndices3 != null)
					System.arraycopy(model.textureTriangleVertexIndices3, 0, merged.textureTriangleVertexIndices3, tPos, model.textureTriangleVertexIndices3.length);
				else
					Arrays.fill(merged.textureTriangleVertexIndices3, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.texturePrimaryColors != null)
					System.arraycopy(model.texturePrimaryColors, 0, merged.texturePrimaryColors, tPos, model.texturePrimaryColors.length);
				else
					Arrays.fill(merged.texturePrimaryColors, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.faceTextures != null)
					System.arraycopy(model.faceTextures, 0, merged.faceTextures, tPos, model.faceTextures.length);
				else
					Arrays.fill(merged.faceTextures, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.textureCoordinates != null)
					System.arraycopy(model.textureCoordinates, 0, merged.textureCoordinates, tPos, model.textureCoordinates.length);
				else
					Arrays.fill(merged.textureCoordinates, tPos, tPos + model.textureTriangleCount, (byte) 0);
				if(model.textureRenderTypes != null)
					System.arraycopy(model.textureRenderTypes, 0, merged.textureRenderTypes, tPos, model.textureRenderTypes.length);
				else
					Arrays.fill(merged.textureRenderTypes, tPos, tPos + model.textureTriangleCount, (byte) 0);
				tPos += model.textureTriangleCount;
			}
			
		}
		
		merged.computeNormals();
		merged.computeTextureUVCoordinates();
		
		return merged;
	}
	
	private static void exportModel(Store store, FrameDefinition frame, ModelDefinition model) throws IOException {
		if(model == null)
			return;
		
		TextureManager tm = new TextureManager(store);
		tm.load();
		
		if(frame != null) {
			model.computeAnimationTables();
			model.animate(frame);
			//model.computeNormals();
		}
		model.rotate4();
		model.resize(sizeX, sizeY, sizeX);
		model.computeNormals();
		model.computeTextureUVCoordinates();
		//model.rotate4();
		ObjExporter exporter = new ObjExporter(tm, model);
		try (PrintWriter objWriter = new PrintWriter(new FileWriter(new File(model.id + ".obj")));
			PrintWriter mtlWriter = new PrintWriter(new FileWriter(new File(model.id + ".mtl"))))
		{
			exporter.export(objWriter, mtlWriter);
			System.out.println("Saved " + model.id + ".obj");
		}
	}
	
	private static void exportModel(Store store, FrameDefinition frame, int modelId, String tag) throws IOException {
		if(modelId < 0)
			return;
		
		TextureManager tm = new TextureManager(store);
		tm.load();
		
		ModelDefinition model = getModel(store, modelId);
		if(frame != null) {
			model.computeAnimationTables();
			model.animate(frame);
			//model.computeNormals();
		}
		model.rotate4();
		model.resize(sizeX, sizeY, sizeX);
		model.computeNormals();
		model.computeTextureUVCoordinates();
		//model.rotate4();
		ObjExporter exporter = new ObjExporter(tm, model);
		try (PrintWriter objWriter = new PrintWriter(new FileWriter(new File(tag + ".obj")));
			PrintWriter mtlWriter = new PrintWriter(new FileWriter(new File(tag + ".mtl"))))
		{
			exporter.export(objWriter, mtlWriter);
			System.out.println("Saved " + tag + ".obj");
		}
	}
	
	private static ModelDefinition getModel(Store store, int modelId) throws IOException {
		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.MODELS);
		
		Archive archive = index.getArchive(modelId);

		byte[] contents = archive.decompress(storage.loadArchive(archive));

		ModelLoader loader = new ModelLoader();
		return loader.load(archive.getArchiveId(), contents);
	}
}
