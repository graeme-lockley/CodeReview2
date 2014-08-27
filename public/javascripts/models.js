var loggedOnUser = undefined;

function templateOnName(name) {
    var templateResult = undefined;

    $.ajax({
        type: "GET",
        url: "/assets/html/" + name,
        async: false,
        success: function (data, status, jqXHR) {
            templateResult = data;
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });

    return templateResult;
}

var Author = Backbone.Model.extend({
    urlRoot: "/authors"
});

var Authors = Backbone.Collection.extend({
    model: Author,
    url: "/authors"
});

var Revisions = Backbone.Model.extend({
    urlRoot: "/revisions",

    reviewStatus: function () {
        return this.get("review").state;
    },
    reviewAuthor: function () {
        if (this.get("review").author == undefined) {
            return undefined;
        } else {
            var reviewAuthor = new Author({id: this.get("review").author.id});
            reviewAuthor.fetch({async: false});
            return reviewAuthor;
        }
    },
    canReview: function () {
        return this.isReviewOutstanding();
    },
    verbs: function () {
        return this.get("verbs");
    },
    verb: function (name, handler) {
        var revision = this;
        $.ajax({
            type: "GET",
            dataType: "json",
            url: this.verbs()[name],
            success: function (data, status, jqXHR) {
                revision.set(data);
            },
            failure: function (f) {
                console.log("Failure", f);
            },
            error: function (e, status) {
                alert("Error: " + e.responseText);
            }
        });
    }
});

function setHtml(domElement, templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    domElement.html(_.template(theTemplate, templateState));
}

function fetchLoggedOnUser(successContinuation) {
    $.ajax({
        type: "GET",
        url: "/auth/user",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            loggedOnUser = data;
            successContinuation();
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });
}

$(document).ready(function () {
    fetchLoggedOnUser(function () {
    });
});
