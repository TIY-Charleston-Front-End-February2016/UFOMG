var templates = {
  feedItem: [
    "<div class='feedItem' data-id='<%= id %>' data-user='<%= userName %>'>",
      "<div class='feedInfoContainer'>",
        "<span class='feedUser'><%= userName %></span>",
        "<span class='feedTime'><%= timestamp %></span>",
        "<span class='feedLocation'><%= location %></span>",
        "<% if (obj.userName === sessionStorage.getItem('userName')) { %>",
          "<div>",
            "<span class='edit'>Edit</span>",
            "<span class='delete'>X</span>",
          "</div>",
        "<% } %>",
      "</div>",
      "<span class='feedImg'><img src='<%= img %>'/></span>",
      "<p><%= text %></p>",
    "</div>"
  ].join(""),
}
