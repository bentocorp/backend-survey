function fetchContent(path) {
  var content = null;
  $.ajax({
    url: path,
    dataType: 'html',
    type: 'GET',
    async: false,
    success: function (data, status, jqXHR) { 
      content = jqXHR.responseText;
    },
    error: function (jqXHR, status, error) { 
      console.log(error);
    }
  });
  
  return content;
}