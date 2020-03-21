var View = {
    id:"",
    div:"",
}

function textView(id,text,cls='title_text') {
    var div = "<div class="+cls+" "+"id="+id+">"+text+"</div>";
    return div;
}

function imageView(id,pic,cls='square_image_50px',err) {
   return "<img id='"+id+"' class='"+cls+"' src='" + pic + "' onerror='"+err+"'>";
}

function listView(id,ori) {
    
}
