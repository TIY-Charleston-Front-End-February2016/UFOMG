var ufomg =  {
  init: function(){
    ufomg.presentation();
    ufomg.events();
  },
  config: {
    feedData: [],
    pollingFunction: undefined,
    geoKey: "AIzaSyBbfNbCqRj5EvUHiGcKahKFu49CYetrxaE",
    geoRoute: "https://maps.googleapis.com/maps/api/js?key=",
    geocoder: undefined,
    geocoderResult: undefined,
  },
  presentation: function(){
    $.stellar();
    ufomg.initializeGeocoder();
  },
  events: function(){
    $('button[name="submit"]').on('click', ufomg.login);
    $('button[name="addNew"]').on('click', ufomg.addUser);
    $('button[name="submitNewSighting"]').on('click', function(event){
      ufomg.addFeed(ufomg.getFeedData(event));
    });
    $('button[name="Add-new-entry"]').on('click', function(event){
      ufomg.showAddForm(event);
    });
    $('button[name="logout"]').on('click', function(event){ufomg.logout(event)});
    $('.feed').on('click', '.delete', function(event){
      ufomg.deleteFeed({id:parseInt($(this).closest('.feedItem').data('id'))});
      ufomg.getFeed();
    });
    $('.feed').on('click', '.edit', function(){});
  },
  getTemplate: function(templateName){
    return templates[templateName];
  },
  constructTemplate: function(templateName, str){
    var tmpl = _.template(ufomg.getTemplate(templateName));
    return tmpl(str);
  },
  buildFeedRow: function(templateName, data, target){
    var output = ufomg.constructTemplate(templateName, data);
    $(target).prepend(output);
  },
  buildFeed: function(array, target, templateName){
    array.forEach(function(el){
      ufomg.buildFeedRow(templateName, el, target);
    });
  },
  login: function(){
    var login = ufomg.getLogin();
    $.ajax({
      url: "/login",
      method: "POST",
      data: login,
      success: function(result){
        var output = result.split(" ");
        sessionStorage.setItem("userID", output[1]);
        sessionStorage.setItem("userName", output[0]);
        $('.login').addClass('hidden');
        $('.mainContent').removeClass('hidden');
        ufomg.getFeed();
      },
      error: function(error){
        console.log("Login error", error);
      }
    })
  },
  getLogin: function(){
    var username = $('input[name="username"]').val();
    var password = $('input[name="password"]').val();
    $('.login-form input').val('');
    return {
      userName: username,
      userPass: password
    };
  },
  logout: function(event){
    $.ajax({
      url: "/logout",
      method: "POST",
      success: function(result) {
        sessionStorage.setItem("userID", "");
        sessionStorage.setItem("userName", "");
        $('.mainContent').addClass('hidden');
        $('.login').removeClass('hidden');
        $('.feed').html("<h2> Loading Feed...</h2>");
      },
      error: function(error) {
        console.log("Logout error", error);
      }
    });
  },
  addUser: function(){
    var user = ufomg.getLogin();
    $.ajax({
      url: "/create-user",
      method: "POST",
      data: user,
      success: function(result){
        console.log(result);
      },
      error: function(error){
        console.log("Add User", error);
      }
    });
  },
  getUser: function(data) {
    $.ajax({
      url: "/allUsers",
      method: "GET",
      success: function(result){

      },
      error: function(error){
        console.log("Get User", error);
      }
    });
  },
  getFeed: function(flag) {
    $.ajax({
      url: "/allSightings",
      method: "GET",
      success: function(result){
        var filtered = ufomg.mapIncomingFeedData(JSON.parse(result));
        if(result.length !== ufomg.config.feedData.length) {
          $('.feed').html('');
          ufomg.buildFeed(filtered, '.feed', 'feedItem');
        }
        if(flag){
          ufomg.buildFeed(filtered, '.feed', 'feedItem');
          $('.feed').html('');
        }
      },
      complete: function() {

      },
      error: function(error){
        console.log("Get Feed", error);
      }
    });
  },
  editFeed: function(data) {
    $.ajax({
      url: "/update-sighting",
      method: "PUT",
      data: data,
      success: function(result){
        ufomg.getFeed(true);
      },
      error: function(error){
        console.log("Edit Feed", error);
      }
    });
  },
  displayEditFeed: function() {

  },
  addFeed: function(data) {
    $.ajax({
      url: "/create-sighting",
      method: "POST",
      data: data,
      success: function(result){
        ufomg.getFeed();
      },
      error: function(error){
        console.log("Add Feed", error);
      }
    });
  },
  mapIncomingFeedData: function(array) {
    var result = array.map(function(el){
      return {
        location: [el.lat,el.lon],
        timestamp: moment.unix(el.timestamp).format("dddd, MMMM Do YYYY, h:mm:ss a"),
        userName: el.userName,
        img: el.url,
        text: el.text,
        id: el.id
      }
    });
    return result;
  },
  getFeedData: function(event){
    event.preventDefault();
    var latlong = $('input[name="location"]').val();
    var img = $('input[name="image"]').val();
    var timestamp = moment().unix();
    var text = $('textarea[name="text"]').val();
    $('.entry-fields').children().val('');
    $('.new-entry').addClass('hidden');
    return {
      lat: latlong,
      lon: latlong,
      text: text,
      timestamp: moment().unix(),
      url: img,
    };
  },
  showAddForm: function(event){
    event.preventDefault();
    $('.new-entry').toggleClass('hidden');
  },
  deleteFeed: function(data) {
    $.ajax({
      url: "/delete-sighting",
      method: "POST",
      data: data,
      success: function(result){

      },
      error: function(error){
        console.log("Delete Feed", error);
      }
    });
  },
  checkDataUpdates: function(){
    ufomg.pollData(ufomg.getFeed, 1000, true);
  },
  pollData: function(callback, pollRate, flag){
    if(flag){
      ufomg.config.pollingFunction = setInterval(callback, pollRate);
    } else if(!flag) {
      clearInterval(ufomg.config.pollingFunction)
    }
  },
  initializeGeocoder: function(){
    ufomg.config.geocoder = new google.maps.Geocoder();
  },
  codeAddress: function(address){
    var results = ufomg.config.geocoder.geocode({ 'address': address }, function(results, status){
      if(status === google.maps.GeocoderStatus.OK) {
        return results;
      } else {
        console.log("Geocoder error", status);
      }
    });
    return results;
  },
  decodeAddress: function(coords){
    var latlong = {
      lat: parseFloat(coords[0]),
      lng: parseFloat(coords[1])
    }
    var results = ufomg.config.geocoder.geocode({'location': latlong}, function(results, status){
      if(status === google.maps.GeocoderStatus.OK) {
        return results;
      } else {
        console.log("Reverse Geocoder error", status)
      }
    });
    return results;
  }
}

$(document).ready(function(){
  ufomg.init();
})
