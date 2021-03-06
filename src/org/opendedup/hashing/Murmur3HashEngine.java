/*******************************************************************************
 * Copyright (C) 2016 Sam Silverberg sam.silverberg@gmail.com	
 *
 * This file is part of OpenDedupe SDFS.
 *
 * OpenDedupe SDFS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenDedupe SDFS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.opendedup.hashing;

import org.opendedup.sdfs.Main;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Murmur3HashEngine implements AbstractHashEngine {

	public static int defaultSeed = 6442;
	private int seed;
	HashFunction hf = null;

	public Murmur3HashEngine() {
		this.seed = defaultSeed;
		hf = Hashing.murmur3_128(seed);
	}

	@Override
	public byte[] getHash(byte[] data) {
		byte[] hash = hf.hashBytes(data).asBytes();
		return hash;
	}

	public static int getHashLenth() {
		// TODO Auto-generated method stub
		return 16;
	}

	@Override
	public void destroy() {
	}

	@Override
	public boolean isVariableLength() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getMaxLen() {
		// TODO Auto-generated method stub
		return Main.CHUNK_LENGTH;
	}

	@Override
	public int getMinLen() {
		// TODO Auto-generated method stub
		return Main.CHUNK_LENGTH;
	}

	@Override
	public void setSeed(int seed) {
		this.seed = seed;
		hf = Hashing.murmur3_128(seed);
		
	}
}
