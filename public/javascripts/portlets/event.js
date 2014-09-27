$(document).ready(function () {
    var EventView = Backbone.View.extend({
        events: {
            "click": "select"
        },
        select: function () {
            if ($(event.target).attr("data-url") !== undefined) {
                window.location = $(event.target).attr("data-url");
            } else {
                var detailElement = this.$el.find(".cr-hidden-detail");
                if (detailElement.hasClass("hide")) {
                    detailElement.removeClass("hide");
                } else {
                    detailElement.addClass("hide");
                }
            }
            return false;
        },
        render: function () {
            var prefix = this.$el.attr("id");
            this.$el.html(_.template(templateOnName("portlets/events/" + this.model.get("name") + ".html"), {event: this.model, prefix: prefix}));
        }
    });

    var EventsView = Backbone.View.extend({
        initialize: function () {
            this.listenTo(this.model, 'sync', this.render);
            this.model.fetch();
        },
        render: function () {
            var prefix = this.$el.attr("id");
            this.$el.html(_.template(templateOnName("portlets/events/main.html"), {events: this.model, prefix: prefix}));

            this.model.forEach(function (event) {
                var x = new EventView({model: event, el: "#" + prefix + "-" + event.id});
                x.render();
            });

            $("abbr.timeago").timeago();

            return this;
        }
    });

    $(".events").each(function (idx, dom) {
        var events = new Events();
        events.url = $(dom).attr("data-collection");
        var eventsView = new EventsView({model: events, el: "#" + $(dom).attr("id")});
    });
});