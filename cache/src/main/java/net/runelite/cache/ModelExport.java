package net.runelite.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

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
import net.runelite.cache.models.ObjExporter;

public class ModelExport
{
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
		for(int id : modelIds) {
			exportModel(store, frame, id, String.valueOf(id));
		}
	}
	
	private static void exportModel(Store store, FrameDefinition frame, int modelId, String tag) throws IOException {
		if(modelId < 0)
			return;
		
		TextureManager tm = new TextureManager(store);
		tm.load();
		
		ModelDefinition model = getModel(store, modelId);
		if(frame != null) {
			model.animate(frame);
			//model.computeNormals();
		}
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
