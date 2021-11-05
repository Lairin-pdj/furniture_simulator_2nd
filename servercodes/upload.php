<?php 

    error_reporting(E_ALL); 
    ini_set('display_errors',1); 

    include('dbcon.php');

    $name = urldecode($_POST['name']);
    $fileobj = $_FILES['obj'];
    $filepre = $_FILES['pre'];
    $filetex = $_FILES['tex'];

    $objname = $name.".obj";
    $objtemp = $fileobj['tmp_name'];
    $prename = $name."_preview.png";
    $pretemp = $filepre['tmp_name'];
    $texname = $name.".png";
    $textemp = $filetex['tmp_name'];

    $dst = "furniture/".$name;
    if(!file_exists($dst)){
	mkdir($dst);
	
        $objdst = $dst."/".$objname;
	$reobj = move_uploaded_file($objtemp, $objdst);
	$predst = $dst."/".$prename;
        $repre = move_uploaded_file($pretemp, $predst);
        $texdst = $dst."/".$texname;
	$retex = move_uploaded_file($textemp, $texdst);

	$dstobj = "/".$objdst;
	$dstpre = "/".$predst;
	$dsttex = "/".$texdst;
	$stmt = $con->prepare("insert into furniture(name, extension, preview_link, file_link, texture_link, viewer_count, download_count) 
			       value('$name', 'obj', '$dstpre', '$dstobj', '$dsttex', 0, 0)");
        $redb = $stmt->execute();

        if($reobj and $repre and $retex and $redb){
            echo "$name";
        }

    }else{
	echo "already exist";
    }	
?>
