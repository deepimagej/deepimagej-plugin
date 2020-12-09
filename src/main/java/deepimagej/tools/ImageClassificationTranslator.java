/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package deepimagej.tools;

import ai.djl.modality.Classifications;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import java.io.IOException;
import java.util.List;
public class ImageClassificationTranslator implements Translator<NDList, NDList> {
    private List<String> classes;

    @Override
    public Batchifier getBatchifier() {
        return null;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, NDList input) throws IOException {
    	return input;
    }

    @Override
    public NDList processOutput(TranslatorContext ctx, NDList list) {
        NDArray probabilitiesNd = list.singletonOrThrow();
        probabilitiesNd = probabilitiesNd.softmax(0);
        return new NDList(probabilitiesNd);
    }

}