<?php 

    error_reporting(E_ALL); 
    ini_set('display_errors',1); 

    include('dbcon.php');

    $text = $_POST['text'];
    $stmt = $con->prepare("select * from furniture where name like '%$text%'");
    $stmt->execute();

    if ($stmt->rowCount() > 0)
    {
        $data = array(); 

        while($row=$stmt->fetch(PDO::FETCH_ASSOC))
        {
            extract($row);
    
            array_push($data, 
                array('id'=>$id,
		'name'=>$name,
		'extension'=>$extension,
		'preview_link'=>$preview_link,
		'file_link'=>$file_link,
		'texture_link'=>$texture_link,
		'viewer_count'=>$viewer_count,
		'download_count'=>$download_count
            ));
        }

        header('Content-Type: application/json; charset=utf8');
        $json = json_encode(array("furnitures"=>$data), JSON_PRETTY_PRINT+JSON_UNESCAPED_UNICODE);
        echo $json;
    }

?>
