/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.tools;

public class NumFormat {
	
	public static String chrono(double chrono) {
		return time(System.nanoTime() - chrono);
	}

	public static String seconds(double ns) {
		return String.format("%5.1f s", ns * 1e-9);
		
	}

	public static String time(double ns) {
		if (ns < 3000.0)
			return String.format("%3.1f ns", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f us", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f ms", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f s", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f s", ns);
		ns /= 3600.0;
		return String.format("%3.1f h", ns);
	}

	public static String bytes(double bytes) {
		if (bytes < 3000)
			return String.format("%3.0f b", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Kb", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Mb", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Gb", bytes);
		bytes /= 1024.0;
		return String.format("%3.1f Tb", bytes);
	}

	public static String toPercent(String value) {
		try {
			return toPercent(Double.parseDouble(value));
		}
		catch(Exception ex) {}
		return value;
	}

	public static String toPercent(double value) {
		return String.format("%5.3f", value * 100) + "%";
	}

}
