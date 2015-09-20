/*
 * This file is part of Restructured, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.mod.Restructured.world.village.themes;

import java.util.HashMap;
import java.util.Map;

import org.blockartistry.mod.Restructured.util.SelectedBlock;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.BiomeEvent;

public class VillageTheme {

	protected static final HashMap<BiomeGenBase, VillageTheme> themes = new HashMap<BiomeGenBase, VillageTheme>();
	protected static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();
	protected static final int META_MASK = 0xFF;
	protected static final int BLOCK_SHIFT = 8;

	public static final VillageTheme DEFAULT_THEME = new VillageTheme(BiomeGenBase.plains);

	protected static void register(BiomeGenBase biome, VillageTheme theme) {
		themes.put(biome, theme);
	}
	
	public static VillageTheme find(BiomeGenBase biome) {
		final VillageTheme vt = themes.get(biome);
		return vt == null ? DEFAULT_THEME : vt;
	}
	
	protected static int encode(Block block, int meta) {
		return BLOCK_REGISTRY.getId(block) << BLOCK_SHIFT | (meta & META_MASK);
	}
	
	protected static Block decodeBlock(int code) {
		return BLOCK_REGISTRY.getObjectById(code >> BLOCK_SHIFT);
	}
	
	protected static int decodeMeta(int code) {
		return code & META_MASK;
	}

	protected final Map<Block, Integer> blockReplacements;
	protected final BiomeGenBase biome;
	
	protected VillageTheme(BiomeGenBase biome) {
		this(biome, new HashMap<Block, Integer>());
	}
	
	protected VillageTheme(BiomeGenBase biome, Map<Block, Integer> replacements) {
		this.biome = biome;
		this.blockReplacements = replacements;
	}
	
	public BiomeGenBase getBiome() {
		return this.biome;
	}
	
	protected Block biomeBlockReplace(SelectedBlock block) {

		final BiomeEvent.GetVillageBlockID event = new BiomeEvent.GetVillageBlockID(
				biome, block.getBlock(), block.getMeta());
		MinecraftForge.TERRAIN_GEN_BUS.post(event);
		if (event.getResult() == Result.DENY)
			return event.replacement;
		
		Block replace = null;
		Integer code = blockReplacements.get(block.getBlock());
		if(code != null)
			replace = decodeBlock(code);
		
		return (replace == null) ? block.getBlock() : replace;
	}
	
	protected int biomeMetaReplace(SelectedBlock block) {

		final BiomeEvent.GetVillageBlockMeta event = new BiomeEvent.GetVillageBlockMeta(
				biome, block.getBlock(), block.getMeta());
		MinecraftForge.TERRAIN_GEN_BUS.post(event);
		if (event.getResult() == Result.DENY)
			return event.replacement;
		
		int replace = META_MASK;
		final Integer code = blockReplacements.get(block.getBlock());
		if(code != null)
			replace = decodeMeta(code);

		if(replace != META_MASK) {
			// Preserve slab orientation
			if(block.isSlab())
				replace |= (block.getMeta() & 8);
			// Preserve log orientation
			else if(block.isLog())
				replace |= (block.getMeta() & 12);
		}

		return replace != META_MASK ? replace : block.getMeta();
	}
	
	protected static SelectedBlock scrubEggs(Block block, int meta) {
		// If it is a monster egg scrub it
		if (block == Blocks.monster_egg)
			switch (meta) {
			case 0:
				block = Blocks.stone;
				meta = 0;
				break;
			case 1:
				block = Blocks.cobblestone;
				meta = 0;
				break;
			case 2:
				block = Blocks.stonebrick;
				meta = 0;
				break;
			case 3:
				block = Blocks.stonebrick;
				meta = 1;
				break;
			case 4:
				block = Blocks.stonebrick;
				meta = 2;
			case 5:
				block = Blocks.stonebrick;
				meta = 3;
				break;
			default:
				;
			}
		return new SelectedBlock(block, meta);
	}

	public SelectedBlock findReplacement(Block block, int meta, boolean scrubEggs) {
		
		SelectedBlock t = null;
		
		if(scrubEggs)
			t = scrubEggs(block, meta);
		else
			t = new SelectedBlock(block, meta);

		final Block b = biomeBlockReplace(t);
		final int m = biomeMetaReplace(t);
		return new SelectedBlock(b, m);
	}
	
	public Block findReplacementBlock(Block block, int meta, boolean scrubEggs) {
		
		SelectedBlock t = null;
		
		if(scrubEggs)
			t = scrubEggs(block, meta);
		else
			t = new SelectedBlock(block, meta);

		return biomeBlockReplace(t);
	}
	
	public int findReplacementMeta(Block block, int meta, boolean scrubEggs) {
		
		SelectedBlock t = null;
		
		if(scrubEggs)
			t = scrubEggs(block, meta);
		else
			t = new SelectedBlock(block, meta);

		return biomeMetaReplace(t);
	}
}
