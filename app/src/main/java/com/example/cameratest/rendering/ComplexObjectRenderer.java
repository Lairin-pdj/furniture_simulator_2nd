package com.example.cameratest.rendering;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.javagl.obj.Mtl;
import de.javagl.obj.MtlReader;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjSplitting;
import de.javagl.obj.ObjUtils;

public class ComplexObjectRenderer
{
    private static final String TAG =
            ComplexObjectRenderer.class.getSimpleName();

    private final List<ObjectRenderer> materialGroupObjectRenderers;

    public ComplexObjectRenderer()
    {
        this.materialGroupObjectRenderers = new ArrayList<>();
    }


    public void createOnGlThread(Context context, String objAssetName,
                                 String defaultTextureFileName) throws IOException
    {
        InputStream objInputStream = context.getAssets().open(objAssetName);
        Obj obj = ObjReader.read(objInputStream);
        Obj renderableObj = ObjUtils.convertToRenderable(obj);

        if (renderableObj.getNumMaterialGroups() == 0)
        {
            createRenderers(context, renderableObj, defaultTextureFileName);
        }
        else
        {
            // Otherwise, create one renderer for each material
            createMaterialBasedRenderers(context, renderableObj,
                    defaultTextureFileName);
        }
    }

    private void createRenderers(Context context, Obj obj,
                                 String textureFileName) throws IOException
    {
        if (obj.getNumVertices() <= 65000)
        {
            createRenderer(context, obj, textureFileName);
        }
        else
        {
            // If there are more than 65k vertices, then the object has to be
            // split into multiple parts, each having at most 65k vertices
            List<Obj> objParts = ObjSplitting.splitByMaxNumVertices(obj, 65000);
            for (int j = 0; j < objParts.size(); j++)
            {
                Obj objPart = objParts.get(j);
                createRenderer(context, objPart, textureFileName);
            }
        }
    }

    private void createMaterialBasedRenderers(Context context, Obj obj,
                                              String defaultTextureFileName) throws IOException
    {

        // Read the MTL files that are referred to from the OBJ, and
        // extract all their MTL definitions
        List<String> mtlFileNames = obj.getMtlFileNames();
        List<Mtl> allMtls = new ArrayList<>();
        for (String mtlFileName : mtlFileNames)
        {
            InputStream mtlInputStream = context.getAssets().open("models/" + mtlFileName);
            List<Mtl> mtls = MtlReader.read(mtlInputStream);
            allMtls.addAll(mtls);
        }

        // Obtain the material groups from the OBJ, and create renderers for
        // each of them
        Map<String, Obj> materialGroupObjs =
                ObjSplitting.splitByMaterialGroups(obj);
        for (Map.Entry<String, Obj> entry : materialGroupObjs.entrySet())
        {
            String materialName = entry.getKey();
            String textureFileName = findTextureFileName(materialName, allMtls,
                    defaultTextureFileName);
            Obj materialGroupObj = entry.getValue();
            createRenderers(context, materialGroupObj, textureFileName);
        }
    }


    private String findTextureFileName(String materialName,
                                       Iterable<? extends Mtl> mtls, String defaultTextureFileName)
    {
        for (Mtl mtl : mtls)
        {
            if (Objects.equals(materialName, mtl.getName()))
            {
                return mtl.getMapKd();
            }
        }
        return defaultTextureFileName;
    }

    private void createRenderer(Context context, Obj obj,
                                String textureFileName) throws IOException
    {

        Log.i(TAG, "Rendering part with " + obj.getNumVertices()
                + " vertices and " + textureFileName);

        ObjectRenderer objectRenderer = new ObjectRenderer();
        objectRenderer.createOnGlThread(context, obj, textureFileName);
        materialGroupObjectRenderers.add(objectRenderer);
    }

    public void draw(float[] cameraView, float[] cameraPerspective,
                     float[] lightIntensity)
    {
        for (ObjectRenderer renderer : materialGroupObjectRenderers)
        {
            renderer.draw(cameraView, cameraPerspective, lightIntensity);
            Log.e("Asdfasdf","asdfasdf");
        }
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        for(ObjectRenderer i : materialGroupObjectRenderers){
            i.updateModelMatrix(modelMatrix, scaleFactor);
        }
    }
}